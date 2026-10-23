package com.pgmanagement.service;



import com.pgmanagement.constants.BillingErrorMessages;
import com.pgmanagement.dto.request.BillChargeRequest;
import com.pgmanagement.dto.response.BillChargeResponse;
import com.pgmanagement.dto.response.BillDetailsResponse;
import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.dto.response.BillVersionResponse;
import com.pgmanagement.dto.request.EditBillRequest;
import com.pgmanagement.entity.*;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillEditService {

    private final BillRepository billRepository;
    private final BillChargeRepository billChargeRepository;
    private final BillVersionRepository billVersionRepository;
    private final PGBillingConfigRepository billingConfigRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PGRepository pgRepository;
    private final BillCalculationService billCalculationService;
    private final UpiPaymentLinkService upiPaymentLinkService;

    /**
     * Edit a bill (owner-only), capturing a new version.
     */
    @Transactional
    public BillDetailsResponse editBill(Long ownerId, Long billId, EditBillRequest request) throws IllegalStateException {
        log.info("Owner {} editing bill {}", ownerId, billId);

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.BILL_NOT_FOUND, billId)));

        if (!bill.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_BILL_OWNER);
        }

        if (bill.getStatus() == BillStatus.PAID || bill.getStatus() == BillStatus.SETTLED) {
            throw new IllegalStateException(BillingErrorMessages.BILL_ALREADY_PAID);
        }

        PGBillingConfig config = billingConfigRepository.findByPgId(bill.getPgId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CONFIG_NOT_FOUND, bill.getPgId())));

        // Save current state as a version BEFORE changes
        saveBillVersion(bill, request.getEditReason(), ownerId);

        // Apply edits
        if (request.getRoomRent() != null) bill.setRoomRent(request.getRoomRent());
        if (request.getElectricityCharge() != null) bill.setElectricityCharge(request.getElectricityCharge());
        if (request.getDiscount() != null) bill.setDiscount(request.getDiscount());
        if (request.getLateFee() != null) bill.setLateFee(request.getLateFee());
        bill.setAdditionalDeductions(request.getAdditionalDeductions());

        // Replace standard charges if provided
        if (request.getStandardCharges() != null) {
            billChargeRepository.deleteByBillId(bill.getId());
            for (BillChargeRequest c : request.getStandardCharges()) {
                BillCharge charge = new BillCharge();
                charge.setBillId(bill.getId());
                charge.setChargeName(c.getName());
                charge.setAmount(c.getAmount());
                charge.setIsProRated(false);
                billChargeRepository.save(charge);
            }

            BigDecimal newOtherCharges = request.getStandardCharges().stream()
                    .map(BillChargeRequest::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            bill.setOtherCharges(newOtherCharges);
        }

        // Recalculate subtotal, GST, total
        BillCalculationService.BillTotals totals = billCalculationService.calculateBillTotals(
                bill.getRoomRent(),
                bill.getElectricityCharge(),
                bill.getOtherCharges(),
                bill.getDiscount() != null ? bill.getDiscount() : BigDecimal.ZERO,
                bill.getLateFee() != null ? bill.getLateFee() : BigDecimal.ZERO,
                bill.getAdditionalDeductions() != null ? bill.getAdditionalDeductions() : BigDecimal.ZERO,
                config
        );

        bill.setSubtotal(totals.subtotal());
        bill.setGstEnabled(config.getGstEnabled());
        bill.setGstPercentage(config.getGstPercentage());
        bill.setGstAmount(totals.gstAmount());
        bill.setTotalAmount(totals.totalAmount());
        bill.setVersion(bill.getVersion() + 1);
        bill.setUpdatedAt(LocalDateTime.now());

        billRepository.save(bill);

        // Build details response
        return buildBillDetailsResponse(bill);
    }

    /**
     * Get full bill details including charges, versions, and payment history.
     */
    @Transactional(readOnly = true)
    public BillDetailsResponse getBillDetails(Long ownerId, Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.BILL_NOT_FOUND, billId)));

        if (!bill.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_BILL_OWNER);
        }

        return buildBillDetailsResponse(bill);
    }

    // ========== HELPER METHODS ==========

    private void saveBillVersion(Bill bill, String reason, Long ownerId) {
        BillVersion version = new BillVersion();
        version.setBillId(bill.getId());
        version.setVersionNumber(bill.getVersion());
        version.setTotalAmount(bill.getTotalAmount());
        version.setRoomRent(bill.getRoomRent());
        version.setElectricityCharge(bill.getElectricityCharge());
        version.setOtherCharges(bill.getOtherCharges());
        version.setDiscount(bill.getDiscount());
        version.setLateFee(bill.getLateFee());
        version.setGstAmount(bill.getGstAmount());
        version.setChangeReason(reason);
        version.setChangedBy(ownerId);
        version.setChangedAt(LocalDateTime.now());

        billVersionRepository.save(version);
        log.info("Saved bill version {} for bill {}", version.getVersionNumber(), bill.getBillNumber());
    }

    private BillDetailsResponse buildBillDetailsResponse(Bill bill) {
        User tenant = userRepository.findById(bill.getTenantId()).orElse(null);
        Room room = roomRepository.findById(bill.getRoomId()).orElse(null);
        PG pg = pgRepository.findById(bill.getPgId()).orElse(null);

        PGBillingConfig config = billingConfigRepository.findByPgId(bill.getPgId()).orElse(null);
        List<BillCharge> charges = billChargeRepository.findByBillId(bill.getId());
        List<BillVersion> versions = billVersionRepository.findByBillIdOrderByVersionNumberAsc(bill.getId());

        List<BillChargeResponse> chargeResponses = charges.stream()
                .map(c -> BillChargeResponse.builder()
                        .id(c.getId())
                        .chargeName(c.getChargeName())
                        .amount(c.getAmount())
                        .isProRated(c.getIsProRated())
                        .build())
                .collect(Collectors.toList());

        List<BillVersionResponse> versionResponses = versions.stream()
                .map(v -> BillVersionResponse.builder()
                        .id(v.getId())
                        .versionNumber(v.getVersionNumber())
                        .totalAmount(v.getTotalAmount())
                        .roomRent(v.getRoomRent())
                        .electricityCharge(v.getElectricityCharge())
                        .otherCharges(v.getOtherCharges())
                        .discount(v.getDiscount())
                        .lateFee(v.getLateFee())
                        .gstAmount(v.getGstAmount())
                        .changeReason(v.getChangeReason())
                        .changedByName(v.getChangedByOwner() != null ? v.getChangedByOwner().getName() : null)
                        .changedAt(v.getChangedAt())
                        .build())
                .collect(Collectors.toList());

        // Payment history and electricity share can be populated here if needed
        return BillDetailsResponse.builder()
                .bill(BillResponse.builder()
                        .id(bill.getId())
                        .billNumber(bill.getBillNumber())
                        .tenantId(bill.getTenantId())
                        .tenantName(tenant != null ? tenant.getName() : null)
                        .tenantEmail(tenant != null ? tenant.getEmail() : null)
                        .roomId(bill.getRoomId())
                        .roomNumber(room != null ? room.getRoomNumber() : null)
                        .pgId(bill.getPgId())
                        .pgName(pg != null ? pg.getName() : null)
                        .billingPeriodStart(bill.getBillingPeriodStart())
                        .billingPeriodEnd(bill.getBillingPeriodEnd())
                        .roomRent(bill.getRoomRent())
                        .electricityCharge(bill.getElectricityCharge())
                        .otherCharges(bill.getOtherCharges())
                        .discount(bill.getDiscount())
                        .lateFee(bill.getLateFee())
                        .additionalDeductions(bill.getAdditionalDeductions())
                        .subtotal(bill.getSubtotal())
                        .gstEnabled(bill.getGstEnabled())
                        .gstPercentage(bill.getGstPercentage())
                        .gstAmount(bill.getGstAmount())
                        .securityDepositUsed(bill.getSecurityDepositUsed())
                        .totalAmount(bill.getTotalAmount())
                        .status(bill.getStatus())
                        .billType(bill.getBillType())
                        .generationType(bill.getGenerationType())
                        .dueDate(bill.getDueDate())
                        .paidDate(bill.getPaidDate())
                        .generatedDate(bill.getGeneratedDate())
                        .version(bill.getVersion())
                        .notes(bill.getNotes())
                        .upiPaymentLink(config != null
                                ? upiPaymentLinkService.generateUpiPaymentLink(bill, config)
                                : null)
                        .createdAt(bill.getCreatedAt())
                        .updatedAt(bill.getUpdatedAt())
                        .build())
                .charges(chargeResponses)
                .versionHistory(versionResponses)
                .build();
    }


    /**
     * Cancel a bill (owner-only)
     */
    @Transactional
    public BillDetailsResponse cancelBill(Long ownerId, Long billId, String cancellationReason)
            throws IllegalStateException {
        log.info("Owner {} cancelling bill {}", ownerId, billId);

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.BILL_NOT_FOUND, billId)));

        if (!bill.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_BILL_OWNER);
        }

        if (bill.getStatus() == BillStatus.PAID || bill.getStatus() == BillStatus.SETTLED) {
            throw new IllegalStateException(BillingErrorMessages.BILL_ALREADY_PAID);
        }

        if (bill.getStatus() == BillStatus.CANCELLED) {
            throw new IllegalStateException("Bill is already cancelled");
        }

        bill.setStatus(BillStatus.CANCELLED);
        bill.setCancellationReason(cancellationReason);
        bill.setUpdatedAt(LocalDateTime.now());

        billRepository.save(bill);
        log.info("Bill {} cancelled successfully", bill.getBillNumber());

        return buildBillDetailsResponse(bill);
    }

}

