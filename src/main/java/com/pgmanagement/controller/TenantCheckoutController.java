package com.pgmanagement.controller;



import com.pgmanagement.dto.response.CheckoutPreviewResponse;
import com.pgmanagement.dto.request.InitiateCheckoutRequest;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.service.CheckoutService;
import com.pgmanagement.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/billing/checkout")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class TenantCheckoutController {

    private final CheckoutService checkoutService;
    private final JwtTokenProvider jwtUtil;

    /**
     * Initiate checkout request
     * POST /api/tenant/billing/checkout/initiate
     */
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, Object>> initiateCheckout(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody InitiateCheckoutRequest request) throws IllegalStateException {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} initiating checkout with move-out date {}",
                tenantId, request.getMoveOutDate());

        checkoutService.initiateCheckout(tenantId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Checkout request submitted successfully. Owner will process your request.");

        return ResponseEntity.ok(result);
    }

    /**
     * Get checkout preview (estimate)
     * GET /api/tenant/billing/checkout/preview
     */
    @GetMapping("/preview")
    public ResponseEntity<Map<String, Object>> getCheckoutPreview(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching checkout preview", tenantId);

        CheckoutPreviewResponse preview = checkoutService.getCheckoutPreview(tenantId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", preview);

        return ResponseEntity.ok(result);
    }
}

