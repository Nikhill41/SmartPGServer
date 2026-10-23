package com.pgmanagement.enums;

public enum BillStatus {
    PENDING,           // Bill generated, awaiting payment
    PAID,              // Payment verified by owner
    OVERDUE,           // Past due date, not paid
    CANCELLED,         // Bill cancelled (e.g., mid-month checkout)
    SETTLED            // Final settlement complete (for FINAL_SETTLEMENT bills)
}
