package com.pgmanagement.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for unpairing a meter (optional, can be empty)
 * Meter ID is taken from path variable
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnpairMeterRequest {

    /**
     * Optional reason for unpairing
     */
    private String reason;
}
