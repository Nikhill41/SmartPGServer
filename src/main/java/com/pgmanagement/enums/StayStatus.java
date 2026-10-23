package com.pgmanagement.enums;

public enum StayStatus {
    ACTIVE,               // Currently living in room
    PENDING_SETTLEMENT,   // Checkout initiated, awaiting final bill settlement
    COMPLETED             // Checkout complete, tenant removed from room
}
