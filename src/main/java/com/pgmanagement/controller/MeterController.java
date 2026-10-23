package com.pgmanagement.controller;

import com.pgmanagement.dto.response.ApiResponse;
import com.pgmanagement.dto.response.LiveMeterResponse;
import com.pgmanagement.dto.response.MeterHistoryResponse;
import com.pgmanagement.security.UserDetailsImpl;
import com.pgmanagement.service.MeterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Unified controller for meter read operations
 * Accessible by both owners and tenants
 */
@RestController
@RequestMapping("/api/meters")
@RequiredArgsConstructor
@Slf4j
public class MeterController {

    private final MeterService meterService;

    /**
     * Get live readings by room ID
     * GET /api/meters/room/{roomId}/live
     *
     * Access: Owner (owns room) OR Tenant (active booking in room)
     */
    @GetMapping("/room/{roomId}/live")
    public ResponseEntity<ApiResponse<LiveMeterResponse>> getLiveByRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("User {} ({}) fetching live readings for room {}",
                userDetails.getId(), userDetails.getRole(), roomId);

        LiveMeterResponse response = meterService.getLiveByRoom(
                roomId, userDetails.getId(), userDetails.getRole());

        return ResponseEntity.ok(ApiResponse.success("Live readings retrieved", response));
    }

    /**
     * Get live readings by meter ID
     * GET /api/meters/{meterId}/live
     *
     * Access: Owner (owns meter) OR Tenant (active booking in meter's room)
     */
    @GetMapping("/{meterId}/live")
    public ResponseEntity<ApiResponse<LiveMeterResponse>> getLiveByMeter(
            @PathVariable String meterId,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("User {} ({}) fetching live readings for meter {}",
                userDetails.getId(), userDetails.getRole(), meterId);

        LiveMeterResponse response = meterService.getLiveByMeter(
                meterId, userDetails.getId(), userDetails.getRole());

        return ResponseEntity.ok(ApiResponse.success("Live readings retrieved", response));
    }

    /**
     * Get historical readings for a meter
     * GET /api/meters/{meterId}/readings?start={startMs}&end={endMs}
     *
     * Access: Owner (owns meter) OR Tenant (active booking in meter's room)
     *
     * Query Parameters:
     * - start: Start timestamp in Unix milliseconds (required)
     * - end: End timestamp in Unix milliseconds (required)
     */
    @GetMapping("/{meterId}/readings")
    public ResponseEntity<ApiResponse<MeterHistoryResponse>> getHistoricalReadings(
            @PathVariable String meterId,
            @RequestParam Long start,
            @RequestParam Long end,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("User {} ({}) fetching history for meter {} from {} to {}",
                userDetails.getId(), userDetails.getRole(), meterId, start, end);

        MeterHistoryResponse response = meterService.getHistory(
                meterId, start, end, userDetails.getId(), userDetails.getRole());

        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved " + response.getSummary().getTotalReadings() + " readings", response));
    }
}
