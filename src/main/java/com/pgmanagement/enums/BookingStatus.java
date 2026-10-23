package com.pgmanagement.enums;

public enum BookingStatus {

    PENDING,     // Awaiting owner decision
    ACCEPTED,    // Owner accepted, tenant assigned
    REJECTED,
    COMPLETED     // Owner rejected with reason
}
