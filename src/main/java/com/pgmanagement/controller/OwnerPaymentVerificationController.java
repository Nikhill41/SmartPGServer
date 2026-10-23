package com.pgmanagement.controller;



import com.pgmanagement.dto.response.PaymentProofHistoryResponse;
import com.pgmanagement.dto.request.PaymentVerificationRequest;
import com.pgmanagement.dto.response.PendingVerificationResponse;
import com.pgmanagement.entity.Bill;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.service.PaymentVerificationService;
import com.pgmanagement.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner/billing/payments")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('OWNER')")
public class OwnerPaymentVerificationController {

    private final PaymentVerificationService paymentVerificationService;
    private final JwtTokenProvider jwtUtil;

    /**
     * Approve payment proof
     * POST /api/owner/billing/payments/{billId}/approve
     */
    @PostMapping("/{billId}/approve")
    public ResponseEntity<Map<String, Object>> approvePayment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId,
            @Valid @RequestBody PaymentVerificationRequest request) throws IllegalStateException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} approving payment for bill {}", ownerId, billId);

        paymentVerificationService.approvePayment(ownerId, billId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Payment verified successfully");

        return ResponseEntity.ok(result);
    }

    /**
     * Reject payment proof
     * POST /api/owner/billing/payments/{billId}/reject
     */
    @PostMapping("/{billId}/reject")
    public ResponseEntity<Map<String, Object>> rejectPayment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId,
            @Valid @RequestBody PaymentVerificationRequest request) throws IllegalStateException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} rejecting payment for bill {}", ownerId, billId);

        paymentVerificationService.rejectPayment(ownerId, billId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Payment proof rejected. Tenant notified to re-upload.");

        return ResponseEntity.ok(result);
    }

    /**
     * Get payment proof history for a bill
     * GET /api/owner/billing/payments/{billId}/history
     */
    @GetMapping("/{billId}/history")
    public ResponseEntity<Map<String, Object>> getPaymentProofHistory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching payment proof history for bill {}", ownerId, billId);

        List<PaymentProofHistoryResponse> history =
                paymentVerificationService.getPaymentProofHistory(billId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", history);

        return ResponseEntity.ok(result);
    }

    /**
     * Get pending payment verifications for PG
     * GET /api/owner/billing/payments/pg/{pgId}/pending
     */
    @GetMapping("/pg/{pgId}/pending")
    public ResponseEntity<Map<String, Object>> getPendingVerifications(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching pending payment verifications for PG {}", ownerId, pgId);

        List<Bill> pendingBills = paymentVerificationService.getPendingVerifications(ownerId, pgId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", pendingBills);
        result.put("count", pendingBills.size());

        return ResponseEntity.ok(result);
    }


    /**
     * Get payment verifications by status for PG
     * GET /api/owner/billing/payments/pg/{pgId}?status=UPLOADED
     */
    @GetMapping("/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> getPaymentsByStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @RequestParam(required = false, defaultValue = "UPLOADED") String status) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching {} payments for PG {}", ownerId, status, pgId);

        List<PendingVerificationResponse> payments =
                paymentVerificationService.getPaymentsByStatusForPg(ownerId, pgId, status);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", payments);
        result.put("count", payments.size());

        return ResponseEntity.ok(result);
    }

}

