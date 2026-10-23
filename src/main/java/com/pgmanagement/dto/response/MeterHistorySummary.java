package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested DTO for meter history summary statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterHistorySummary {

    /**
     * Total number of readings in period
     */
    private Integer totalReadings;

    /**
     * Average voltage
     */
    private Double avgVoltage;

    /**
     * Average current
     */
    private Double avgCurrent;

    /**
     * Average power
     */
    private Double avgPower;

    /**
     * Peak (maximum) power
     */
    private Double peakPower;

    /**
     * Timestamp when peak power occurred
     */
    private Long peakPowerTimestamp;

    /**
     * Energy at start of period
     */
    private Double energyStart;

    /**
     * Energy at end of period
     */
    private Double energyEnd;

    /**
     * Energy consumed during period (kWh)
     */
    private Double energyConsumed;

    /**
     * Average frequency
     */
    private Double avgFrequency;

    /**
     * Average power factor
     */
    private Double avgPowerFactor;
}
