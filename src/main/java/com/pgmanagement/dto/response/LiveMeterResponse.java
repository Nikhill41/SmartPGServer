package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for live meter readings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveMeterResponse {

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
     * Room type (e.g., SINGLE, DOUBLE)
     */
    private String roomType;

    /**
     * Online status
     */
    private Boolean isOnline;

    /**
     * Last reading timestamp (Unix milliseconds)
     */
    private Long lastSeen;

    /**
     * Human-readable timestamp (e.g., "32 seconds ago")
     */
    private String displayTimestamp;

    /**
     * Current meter readings
     */
    private LiveReadings readings;

    /**
     * Current relay status
     */
    private String relayStatus;
}
