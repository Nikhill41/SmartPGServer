package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO for current meter readings
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveReadings {

    /**
     * Voltage in volts
     */
    private Double voltage;

    /**
     * Current in amperes
     */
    private Double current;

    /**
     * Power in watts
     */
    private Double power;

    /**
     * Cumulative energy in kWh
     */
    private Double energy;

    /**
     * Frequency in Hz
     */
    private Double frequency;

    /**
     * Power factor (0.0 to 1.0)
     */
    private Double powerFactor;
}
