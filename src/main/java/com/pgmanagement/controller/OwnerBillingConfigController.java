package com.pgmanagement.controller;



import com.pgmanagement.dto.response.BillingConfigResponse;
import com.pgmanagement.dto.request.CreateBillingConfigRequest;
import com.pgmanagement.dto.request.UpdateBillingConfigRequest;
import com.pgmanagement.exception.DuplicateResourceException;
import com.pgmanagement.service.BillingConfigService;
import com.pgmanagement.security.JwtTokenProvider;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner/billing/config")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('OWNER')")
public class OwnerBillingConfigController {

    private final BillingConfigService billingConfigService;
    private final JwtTokenProvider jwtUtil;

    /**
     * get pg config by pg id
     *
     */





    /**
     * Create billing configuration for a PG
     * POST /api/owner/billing/config
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createBillingConfig(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateBillingConfigRequest request) throws DuplicateResourceException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} creating billing config for PG {}", ownerId, request.getPgId());

        BillingConfigResponse response = billingConfigService.createBillingConfig(ownerId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Billing configuration created successfully");
        result.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Get billing configuration for a PG
     * GET /api/owner/billing/config/pg/{pgId}
     */
    @GetMapping("/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> getBillingConfig(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching billing config for PG {}", ownerId, pgId);

        BillingConfigResponse response = billingConfigService.getBillingConfig(ownerId, pgId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Get all billing configurations for owner
     * GET /api/owner/billing/config
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBillingConfigs(
            @RequestHeader("Authorization") String token) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching all billing configs", ownerId);

        List<BillingConfigResponse> responses = billingConfigService.getAllBillingConfigs(ownerId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", responses);

        return ResponseEntity.ok(result);
    }

    /**
     * Update billing configuration
     * PUT /api/owner/billing/config/pg/{pgId}
     */
    @PutMapping("/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> updateBillingConfig(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @Valid @RequestBody UpdateBillingConfigRequest request) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} updating billing config for PG {}", ownerId, pgId);

        BillingConfigResponse response = billingConfigService.updateBillingConfig(ownerId, pgId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Billing configuration updated successfully");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Delete billing configuration
     * DELETE /api/owner/billing/config/pg/{pgId}
     */
    @DeleteMapping("/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> deleteBillingConfig(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} deleting billing config for PG {}", ownerId, pgId);

        billingConfigService.deleteBillingConfig(ownerId, pgId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Billing configuration deleted successfully");

        return ResponseEntity.ok(result);
    }
}

