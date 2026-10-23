package com.pgmanagement.controller;



import com.pgmanagement.dto.request.CreateStandardChargeRequest;
import com.pgmanagement.dto.response.StandardChargeResponse;
import com.pgmanagement.dto.request.UpdateStandardChargeRequest;
import com.pgmanagement.exception.DuplicateResourceException;
import com.pgmanagement.service.StandardChargeService;
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
@RequestMapping("/api/owner/billing/charges")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('OWNER')")
public class OwnerStandardChargeController {

    private final StandardChargeService standardChargeService;
    private final JwtTokenProvider jwtUtil;

    /**
     * Create standard charge
     * POST /api/owner/billing/charges
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createStandardCharge(
            @RequestHeader("Authorization") String token,
            @Valid @RequestBody CreateStandardChargeRequest request) throws DuplicateResourceException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} creating standard charge '{}' for PG {}",
                ownerId, request.getChargeName(), request.getPgId());

        StandardChargeResponse response = standardChargeService.createStandardCharge(ownerId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Standard charge created successfully");
        result.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Get all standard charges for a PG
     * GET /api/owner/billing/charges/pg/{pgId}
     */
    @GetMapping("/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> getStandardCharges(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching standard charges for PG {}", ownerId, pgId);

        List<StandardChargeResponse> responses = standardChargeService.getStandardCharges(ownerId, pgId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", responses);

        return ResponseEntity.ok(result);
    }

    /**
     * Update standard charge
     * PUT /api/owner/billing/charges/{chargeId}/pg/{pgId}
     */
    @PutMapping("/{chargeId}/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> updateStandardCharge(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @PathVariable Long chargeId,
            @Valid @RequestBody UpdateStandardChargeRequest request) throws DuplicateResourceException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} updating standard charge {} for PG {}", ownerId, chargeId, pgId);

        StandardChargeResponse response = standardChargeService.updateStandardCharge(
                ownerId, pgId, chargeId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Standard charge updated successfully");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Toggle charge active status
     * PATCH /api/owner/billing/charges/{chargeId}/pg/{pgId}/toggle
     */
    @PatchMapping("/{chargeId}/pg/{pgId}/toggle")
    public ResponseEntity<Map<String, Object>> toggleChargeActive(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @PathVariable Long chargeId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} toggling active status for charge {} in PG {}", ownerId, chargeId, pgId);

        StandardChargeResponse response = standardChargeService.toggleChargeActive(ownerId, pgId, chargeId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", response.getIsActive() ? "Charge activated" : "Charge deactivated");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Delete standard charge (soft delete)
     * DELETE /api/owner/billing/charges/{chargeId}/pg/{pgId}
     */
    @DeleteMapping("/{chargeId}/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> deleteStandardCharge(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @PathVariable Long chargeId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} deleting standard charge {} for PG {}", ownerId, chargeId, pgId);

        standardChargeService.deleteStandardCharge(ownerId, pgId, chargeId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Standard charge deleted successfully");

        return ResponseEntity.ok(result);
    }
}

