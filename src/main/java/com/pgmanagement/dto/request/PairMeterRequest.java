package com.pgmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for pairing a meter to a room
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PairMeterRequest {

    /**
     * Unique meter identifier (e.g., "METER_001")
     */
    @NotBlank(message = "Meter ID is required")
    @Pattern(regexp = "^[A-Za-z]+_[A-Za-z0-9_]+$", message = "Meter ID must follow format: Pg_SmartMeter_XXX")
    private String meterId;

    /**
     * Room ID to which the meter will be paired
     */
    @NotNull(message = "Room ID is required")
    private Long roomId;
}
