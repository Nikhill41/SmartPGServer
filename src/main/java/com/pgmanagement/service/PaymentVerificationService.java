package com.pgmanagement.service;



import com.pgmanagement.constants.BillingConstants;
import com.pgmanagement.constants.BillingErrorMessages;
import com.pgmanagement.dto.request.PaymentProofRequest;
import com.pgmanagement.dto.response.PaymentProofHistoryResponse;
import com.pgmanagement.dto.request.PaymentVerificationRequest;
import com.pgmanagement.dto.response.PendingVerificationResponse;
import com.pgmanagement.entity.Bill;
import com.pgmanagement.entity.PaymentProofHistory;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.PaymentProofStatus;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.BillRepository;
import com.pgmanagement.repository.PaymentProofHistoryRepository;
import com.pgmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentVerificationService {

    private final BillRepository billRepository;
    private final PaymentProofHistoryRepository paymentProofHistoryRepository;
    private final S3Service s3Service;
    private final EmailService emailService;
    private final UserRepository userRepository; // ✅ ADD THIS


    /**
     * Upload payment proof by tenant
     */
    @Transactional
    public void uploadPaymentProof(Long tenantId, Long billId, MultipartFile file,
                                   PaymentProofRequest request) throws IllegalStateException {
        log.info("Tenant {} uploading payment proof for bill {}", tenantId, billId);

        // Verify bill belongs to tenant
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.BILL_NOT_FOUND, billId)));

        if (!bill.getTenantId().equals(tenantId)) {
            log.error("Tenant {} attempted to upload proof for bill {} belonging to tenant {}",
                    tenantId, billId, bill.getTenantId());
            throw new ForbiddenException(BillingErrorMessages.NOT_BILL_TENANT);
        }

        // Check if bill is already paid
        if (bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException(BillingErrorMessages.BILL_ALREADY_PAID);
        }

        // Check rejection count
        if (bill.getPaymentRejectionCount() >= BillingConstants.MAX_PAYMENT_REJECTION_ATTEMPTS) {
            throw new IllegalStateException(BillingErrorMessages.MAX_REJECTION_ATTEMPTS_REACHED);
        }

        // Upload to S3
        String s3Key = String.format("%s/tenant-%d/bill-%d/%s",
                BillingConstants.PAYMENT_PROOF_FOLDER, tenantId, billId, file.getOriginalFilename());

        String s3Url;
        try {
            s3Url = s3Service.uploadCustomKeyFile(file, s3Key);
            log.info("Payment proof uploaded to S3: {}", s3Url);
        } catch (Exception e) {
            log.error("Failed to upload payment proof to S3 for bill {}", billId, e);
            throw new RuntimeException(
                    String.format(BillingErrorMessages.PAYMENT_PROOF_UPLOAD_FAILED, e.getMessage()));
        }

        // Update bill
        bill.setPaymentProofUrl(s3Url);
        bill.setPaymentProofUploadedAt(LocalDateTime.now());
        bill.setPaymentProofStatus(PaymentProofStatus.UPLOADED);
        bill.setUpdatedAt(LocalDateTime.now());
        billRepository.save(bill);

        // Save to payment proof history
        PaymentProofHistory history = new PaymentProofHistory();
        history.setBillId(billId);
        history.setProofUrl(s3Url);
        history.setUploadedAt(LocalDateTime.now());
        history.setStatus(PaymentProofStatus.UPLOADED);
        history.setAttemptNumber(bill.getPaymentRejectionCount() + 1);
        history.setPaymentMethod(request.getPaymentMethod());
        history.setTransactionId(request.getTransactionId());
        history.setTransactionDate(request.getTransactionDate());
        history.setNotes(request.getNotes());
        paymentProofHistoryRepository.save(history);

        log.info("Payment proof uploaded successfully for bill {}", billId);

        // Send notification to owner
        emailService.sendPaymentProofUploadedNotification(bill);
    }

    /**
     * Verify payment proof by owner (Approve)
     */
    @Transactional
    public void approvePayment(Long ownerId, Long billId, PaymentVerificationRequest request) throws IllegalStateException {
        log.info("Owner {} approving payment for bill {}", ownerId, billId);

        Bill bill = verifyBillOwnership(ownerId, billId);

        // Validate state
        if (bill.getPaymentProofStatus() != PaymentProofStatus.UPLOADED) {
            throw new IllegalStateException(BillingErrorMessages.PAYMENT_PROOF_NOT_UPLOADED);
        }

        if (bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException(BillingErrorMessages.PAYMENT_PROOF_ALREADY_VERIFIED);
        }

        // Update bill
        bill.setStatus(BillStatus.PAID);
        bill.setPaidDate(request.getPaidDate() != null ? request.getPaidDate() : java.time.LocalDate.now());
        bill.setPaymentProofStatus(PaymentProofStatus.VERIFIED);
        bill.setOwnerVerifiedAt(LocalDateTime.now());
        bill.setOwnerVerificationNotes(request.getVerificationNotes());
        bill.setUpdatedAt(LocalDateTime.now());
        billRepository.save(bill);

        // Update payment proof history
        PaymentProofHistory latestHistory = paymentProofHistoryRepository
                .findLatestByBillId(billId)
                .orElse(null);

        if (latestHistory != null) {
            latestHistory.setStatus(PaymentProofStatus.VERIFIED);
            latestHistory.setVerifiedAt(LocalDateTime.now());
            latestHistory.setVerifiedBy(ownerId);
            paymentProofHistoryRepository.save(latestHistory);
        }

        log.info("Payment approved for bill {}", billId);

        // Send confirmation to tenant
        emailService.sendPaymentConfirmation(bill);
    }

    /**
     * Reject payment proof by owner
     */
    @Transactional
    public void rejectPayment(Long ownerId, Long billId, PaymentVerificationRequest request) throws IllegalStateException {
        log.info("Owner {} rejecting payment for bill {}", ownerId, billId);

        Bill bill = verifyBillOwnership(ownerId, billId);

        // Validate state
        if (bill.getPaymentProofStatus() != PaymentProofStatus.UPLOADED) {
            throw new IllegalStateException(BillingErrorMessages.PAYMENT_PROOF_NOT_UPLOADED);
        }

        if (bill.getStatus() == BillStatus.PAID) {
            throw new IllegalStateException(BillingErrorMessages.PAYMENT_PROOF_ALREADY_VERIFIED);
        }

        // Validate rejection reason
        if (request.getRejectionReason() == null || request.getRejectionReason().isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        // Update bill
        bill.setPaymentProofStatus(PaymentProofStatus.REJECTED);
        bill.setPaymentRejectionCount(bill.getPaymentRejectionCount() + 1);
        bill.setLastRejectionReason(request.getRejectionReason());
        bill.setPaymentProofUrl(null); // Clear rejected proof
        bill.setUpdatedAt(LocalDateTime.now());
        billRepository.save(bill);

        // Update payment proof history
        PaymentProofHistory latestHistory = paymentProofHistoryRepository
                .findLatestByBillId(billId)
                .orElse(null);

        if (latestHistory != null) {
            latestHistory.setStatus(PaymentProofStatus.REJECTED);
            latestHistory.setRejectionReason(request.getRejectionReason());
            latestHistory.setVerifiedAt(LocalDateTime.now());
            latestHistory.setVerifiedBy(ownerId);
            paymentProofHistoryRepository.save(latestHistory);
        }

        log.info("Payment rejected for bill {}, rejection count: {}",
                billId, bill.getPaymentRejectionCount());

        // Send rejection notice to tenant
        emailService.sendPaymentRejection(bill, request.getRejectionReason());
    }

    /**
     * Get payment proof history for a bill
     */
    @Transactional(readOnly = true)
    public List<PaymentProofHistoryResponse> getPaymentProofHistory(Long billId) {
        log.info("Fetching payment proof history for bill {}", billId);

        List<PaymentProofHistory> history = paymentProofHistoryRepository
                .findByBillIdOrderByUploadedAtDesc(billId);

        return history.stream()
                .map(this::mapHistoryToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get pending verifications for owner
     */
    @Transactional(readOnly = true)
    public List<Bill> getPendingVerifications(Long ownerId, Long pgId) {
        log.info("Fetching pending payment verifications for PG {} by owner {}", pgId, ownerId);

        return billRepository.findPendingVerificationsByPgId(pgId);
    }



    /**
     * Get bills by payment proof status for a PG
     */
    @Transactional(readOnly = true)
    public List<PendingVerificationResponse> getPaymentsByStatusForPg(
            Long ownerId, Long pgId, String status) {
        log.info("Fetching {} payment verifications for PG {} by owner {}", status, pgId, ownerId);

        PaymentProofStatus proofStatus = PaymentProofStatus.valueOf(status.toUpperCase());
        List<Bill> bills = billRepository.findByPgIdAndPaymentProofStatus(pgId, proofStatus);

        return bills.stream().map(bill -> {
            // Get latest proof history for transaction details
            PaymentProofHistory latestHistory = paymentProofHistoryRepository
                    .findLatestByBillId(bill.getId())
                    .orElse(null);

            // Get tenant name
            String tenantName = userRepository.findById(bill.getTenantId())
                    .map(u -> u.getName())
                    .orElse("Unknown");
            String tenantEmail = userRepository.findById(bill.getTenantId())
                    .map(u -> u.getEmail())
                    .orElse(null);

            return PendingVerificationResponse.builder()
                    .billId(bill.getId())
                    .billNumber(bill.getBillNumber())
                    .tenantId(bill.getTenantId())
                    .tenantName(tenantName)
                    .tenantEmail(tenantEmail)
                    .totalAmount(bill.getTotalAmount())
                    .paymentProofUrl(bill.getPaymentProofUrl() != null
                            ? s3Service.generatePresignedGetUrl(bill.getPaymentProofUrl())
                            : null)
                    .paymentProofStatus(bill.getPaymentProofStatus())
                    .paymentProofUploadedAt(bill.getPaymentProofUploadedAt())
                    .transactionId(latestHistory != null ? latestHistory.getTransactionId() : null)
                    .paymentMethod(latestHistory != null ? latestHistory.getPaymentMethod() : null)
                    .rejectionCount(bill.getPaymentRejectionCount())
                    .lastRejectionReason(bill.getLastRejectionReason())
                    .build();
        }).collect(Collectors.toList());
    }



    // ========== HELPER METHODS ==========

    private Bill verifyBillOwnership(Long ownerId, Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.BILL_NOT_FOUND, billId)));

        if (!bill.getOwnerId().equals(ownerId)) {
            log.error("Owner {} does not own bill {}", ownerId, billId);
            throw new ForbiddenException(BillingErrorMessages.NOT_BILL_OWNER);
        }

        return bill;
    }

    private PaymentProofHistoryResponse mapHistoryToResponse(PaymentProofHistory history) {
        return PaymentProofHistoryResponse.builder()
                .id(history.getId())
                .proofUrl(history.getProofUrl() != null
                        ? s3Service.generatePresignedGetUrl(history.getProofUrl())
                        : null)
                .uploadedAt(history.getUploadedAt())
                .status(history.getStatus())
                .rejectionReason(history.getRejectionReason())
                .verifiedAt(history.getVerifiedAt())
                .verifiedByName(history.getVerifiedBy() != null ? "Owner" : null)
                .attemptNumber(history.getAttemptNumber())
                .paymentMethod(history.getPaymentMethod())
                .transactionId(history.getTransactionId())
                .transactionDate(history.getTransactionDate())
                .notes(history.getNotes())
                .build();
    }
}

