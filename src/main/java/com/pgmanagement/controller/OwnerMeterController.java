package com.pgmanagement.controller;

import com.pgmanagement.dto.request.PairMeterRequest;
import com.pgmanagement.dto.request.RelayCommandRequest;
import com.pgmanagement.dto.response.*;
import com.pgmanagement.security.UserDetailsImpl;
import com.pgmanagement.service.MeterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for owner-only meter operations
 * Handles pairing, unpairing, relay control, and meter listing
 */
@RestController
@RequestMapping("/api/owner/meters")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('OWNER')")
public class OwnerMeterController {

    private final MeterService meterService;

    /**
     * Pair a meter to a room
     * POST /api/owner/meters/pair
     */
    @PostMapping("/pair")
    public ResponseEntity<ApiResponse<PairMeterResponse>> pairMeter(
            @Valid @RequestBody PairMeterRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Owner {} pairing meter {} to room {}",
                userDetails.getId(), request.getMeterId(), request.getRoomId());

        PairMeterResponse response = meterService.pairMeter(request, userDetails.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Meter paired successfully", response));
    }

    /**
     * Unpair a meter from its room
     * POST /api/owner/meters/{meterId}/unpair
     */
    @PostMapping("/{meterId}/unpair")
    public ResponseEntity<ApiResponse<UnpairMeterResponse>> unpairMeter(
            @PathVariable String meterId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Owner {} unpairing meter {}", userDetails.getId(), meterId);

        UnpairMeterResponse response = meterService.unpairMeter(meterId, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success("Meter unpaired successfully", response));
    }

    /**
     * Control relay (turn ON/OFF)
     * POST /api/owner/meters/{meterId}/relay
     */
    @PostMapping("/{meterId}/relay")
    public ResponseEntity<ApiResponse<RelayCommandResponse>> controlRelay(
            @PathVariable String meterId,
            @Valid @RequestBody RelayCommandRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Owner {} controlling relay for meter {}: {}",
                userDetails.getId(), meterId, request.getCommand());

        RelayCommandResponse response = meterService.controlRelay(
                meterId, request, userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success("Relay command executed", response));
    }

    /**
     * Get all meters owned by the logged-in owner
     * GET /api/owner/meters
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MeterBasicInfoDTO>>> getOwnerMeters(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Owner {} fetching all meters", userDetails.getId());

        List<MeterBasicInfoDTO> meters = meterService.getOwnerMeters(userDetails.getId());

        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + meters.size() + " meters", meters));
    }
}
