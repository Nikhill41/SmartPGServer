package com.pgmanagement.controller;



import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.dto.request.ProcessCheckoutRequest;
import com.pgmanagement.dto.request.SettlementCompleteRequest;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.service.CheckoutService;
import com.pgmanagement.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/owner/billing/checkout")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('OWNER')")
public class OwnerCheckoutController {

    private final CheckoutService checkoutService;
    private final JwtTokenProvider jwtUtil;

    /**
     * Process checkout and generate final settlement bill
     * POST /api/owner/billing/checkout/tenant/{tenantId}
     */
    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<Map<String, Object>> processCheckout(
            @RequestHeader("Authorization") String token,
            @PathVariable Long tenantId,
            @Valid @RequestBody ProcessCheckoutRequest request) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} processing checkout for tenant {}", ownerId, tenantId);

        BillResponse response = checkoutService.processCheckout(ownerId, tenantId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Final settlement bill generated successfully");
        result.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Mark checkout as complete (after final bill is paid)
     * POST /api/owner/billing/checkout/complete/{billId}
     */
    @PostMapping("/complete/{billId}")
    public ResponseEntity<Map<String, Object>> completeCheckout(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId,
            @Valid @RequestBody SettlementCompleteRequest request) throws IllegalStateException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} completing checkout for bill {}", ownerId, billId);

        checkoutService.completeCheckout(ownerId, billId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Checkout completed successfully. Tenant removed from room.");

        return ResponseEntity.ok(result);
    }
}
