package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for historical meter readings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterHistoryResponse {

    /**
     * Meter ID
     */
    private String meterId;

    /**
     * Room ID
     */
    private Long roomId;

    /**
     * Room number/name
     */
    private String roomNumber;

    /**
     * Query period information
     */
    private PeriodInfo period;

    /**
     * Summary statistics for the period
     */
    private MeterHistorySummary summary;

    /**
     * List of individual readings
     */
    private List<MeterReadingPoint> readings;
}
