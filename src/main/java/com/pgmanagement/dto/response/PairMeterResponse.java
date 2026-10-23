package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO after pairing a meter to a room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PairMeterResponse {

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Meter ID
     */
    private String meterId;

    /**
     * AWS Thing Name (same as meterId)
     */
    private String awsThingName;

    /**
     * Room ID
     */
    private Long roomId;

    /**
     * Room number/name
     */
    private String roomNumber;

    /**
     * Owner ID
     */
    private Long ownerId;

    /**
     * Meter status after pairing
     */
    private String status;

    /**
     * Pairing timestamp
     */
    private LocalDateTime pairedAt;

    /**
     * Success/error message
     */
    private String message;
}
