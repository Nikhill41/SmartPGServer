package com.pgmanagement.controller;

import com.pgmanagement.dto.response.ApiResponse;
import com.pgmanagement.dto.response.LiveMeterResponse;
import com.pgmanagement.dto.response.MeterHistoryResponse;
import com.pgmanagement.entity.User;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.security.UserDetailsImpl;
import com.pgmanagement.service.MeterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Convenience controller for tenant-specific meter operations
 * Provides easy access to their own room's meter without knowing roomId
 */
@RestController
@RequestMapping("/api/tenant/my-room/meter")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class TenantMeterController {

    private final MeterService meterService;
    private final UserRepository userRepository;

    @PostConstruct
    public void init() {
        log.info("TenantMeterController loaded");
    }

    /**
     * Get live readings for tenant's own room
     * GET /api/tenant/my-room/meter/live
     *
     * Automatically determines roomId from tenant's active booking
     */
    @GetMapping("/live")
    public ResponseEntity<ApiResponse<LiveMeterResponse>> getMyRoomLive(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Tenant {} fetching their room's live meter readings", userDetails.getId());

        // Get tenant's current room
        User tenant = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        if (tenant.getRoom() == null) {
            throw new ResourceNotFoundException("You don't have an active room booking");
        }

        Long roomId = tenant.getRoom().getId();

        LiveMeterResponse response = meterService.getLiveByRoom(
                roomId, userDetails.getId(), userDetails.getRole());

        return ResponseEntity.ok(ApiResponse.success("Live readings retrieved", response));
    }

    /**
     * Get historical readings for tenant's own room
     * GET /api/tenant/my-room/meter/readings?start={startMs}&end={endMs}
     */
    @GetMapping("/readings")
    public ResponseEntity<ApiResponse<MeterHistoryResponse>> getMyRoomHistory(
            @RequestParam Long start,
            @RequestParam Long end,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Tenant {} fetching their room's meter history from {} to {}",
                userDetails.getId(), start, end);

        // Get tenant's current room
        User tenant = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));

        if (tenant.getRoom() == null) {
            throw new ResourceNotFoundException("You don't have an active room booking");
        }

        // First get the meter for this room
        LiveMeterResponse liveData = meterService.getLiveByRoom(
                tenant.getRoom().getId(), userDetails.getId(), userDetails.getRole());

        String meterId = liveData.getMeterId();

        MeterHistoryResponse response = meterService.getHistory(
                meterId, start, end, userDetails.getId(), userDetails.getRole());

        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + response.getSummary().getTotalReadings() + " readings", response));
    }
}
