package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO for individual meter reading point
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterReadingPoint {

    /**
     * Reading timestamp (Unix milliseconds)
     */
    private Long timestamp;

    /**
     * ISO formatted timestamp
     */
    private String timestampIso;

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
     * Power factor
     */
    private Double powerFactor;

    /**
     * Relay status at this reading
     */
    private String relayStatus;
}
