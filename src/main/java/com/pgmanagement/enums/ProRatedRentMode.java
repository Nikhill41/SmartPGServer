package com.pgmanagement.enums;

public enum ProRatedRentMode {
    FULL_MONTH,    // Charge full month rent even for partial stay
    HALF_MONTH,    // Charge half month rent for partial stay
    DAILY          // Calculate rent per day based on days stayed
}
