package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for relay control command
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelayCommandResponse {

    /**
     * Success status
     */
    private Boolean success;

    /**
     * Meter ID
     */
    private String meterId;

    /**
     * Requested relay status
     */
    private String requestedStatus;

    /**
     * Actual relay status after command (if verified)
     */
    private String finalStatus;

    /**
     * Command ID for tracking
     */
    private String commandId;

    /**
     * Timestamp when command was sent
     */
    private Long updatedAt;

    /**
     * Whether status was verified
     */
    private Boolean verified;

    /**
     * Success/error message
     */
    private String message;
}
