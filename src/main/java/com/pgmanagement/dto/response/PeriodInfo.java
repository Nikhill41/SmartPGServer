package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO for query period information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodInfo {

    /**
     * Start timestamp (Unix milliseconds)
     */
    private Long startTimestamp;

    /**
     * End timestamp (Unix milliseconds)
     */
    private Long endTimestamp;

    /**
     * Start time in ISO format
     */
    private String startIso;

    /**
     * End time in ISO format
     */
    private String endIso;

    /**
     * Duration in human-readable format
     */
    private String duration;
}
