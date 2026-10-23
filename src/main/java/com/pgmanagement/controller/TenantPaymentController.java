package com.pgmanagement.controller;



import com.pgmanagement.dto.response.PaymentProofHistoryResponse;
import com.pgmanagement.dto.request.PaymentProofRequest;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.service.PaymentVerificationService;
import com.pgmanagement.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/billing/payments")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class TenantPaymentController {

    private final PaymentVerificationService paymentVerificationService;
    private final JwtTokenProvider jwtUtil;

    /**
     * Upload payment proof
     * POST /api/tenant/billing/payments/{billId}/upload
     */
    @PostMapping("/{billId}/upload")
    public ResponseEntity<Map<String, Object>> uploadPaymentProof(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String transactionDate,
            @RequestParam(required = false) String notes) throws IllegalStateException {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} uploading payment proof for bill {}", tenantId, billId);

        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Payment proof file is required");
        }

        // Create request object
        PaymentProofRequest request = new PaymentProofRequest();
        if (paymentMethod != null) {
            request.setPaymentMethod(
                    com.pgmanagement.enums.PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        }
        request.setTransactionId(transactionId);
        if (transactionDate != null) {
            request.setTransactionDate(java.time.LocalDate.parse(transactionDate));
        }
        request.setNotes(notes);

        paymentVerificationService.uploadPaymentProof(tenantId, billId, file, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Payment proof uploaded successfully. Awaiting owner verification.");

        return ResponseEntity.ok(result);
    }

    /**
     * Get payment proof history for my bill
     * GET /api/tenant/billing/payments/{billId}/history
     */
    @GetMapping("/{billId}/history")
    public ResponseEntity<Map<String, Object>> getPaymentProofHistory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching payment proof history for bill {}", tenantId, billId);

        // Note: PaymentVerificationService should verify tenant ownership in the service layer
        List<PaymentProofHistoryResponse> history =
                paymentVerificationService.getPaymentProofHistory(billId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", history);

        return ResponseEntity.ok(result);
    }
}

