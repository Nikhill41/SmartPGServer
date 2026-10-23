package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO after unpairing a meter
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnpairMeterResponse {

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Meter ID
     */
    private String meterId;

    /**
     * Previous room ID
     */
    private Long roomId;

    /**
     * Meter status after unpairing
     */
    private String status;

    /**
     * Unpairing timestamp
     */
    private LocalDateTime unpairedAt;

    /**
     * Success/error message
     */
    private String message;
}
