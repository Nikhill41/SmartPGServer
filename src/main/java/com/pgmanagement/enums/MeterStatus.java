package com.pgmanagement.enums;

/**
 * Enum representing the status of a smart meter
 */
public enum MeterStatus {
    ACTIVE,      // Meter is paired and operational
    INACTIVE,    // Meter is temporarily disabled
    UNPAIRED     // Meter is not assigned to any room
}
