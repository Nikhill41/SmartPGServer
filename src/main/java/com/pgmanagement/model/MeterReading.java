package com.pgmanagement.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model representing a single meter reading from DynamoDB
 * Maps to the meter_readings table structure
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterReading {

    /**
     * Meter identifier (partition key)
     */
    private String meterId;

    /**
     * Reading timestamp in Unix milliseconds (sort key)
     */
    private Long timestamp;

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

    /**
     * Relay status: "ON" or "OFF"
     */
    private String relayStatus;
}
