package com.pgmanagement.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingDashboardResponse {

    private Long pgId;
    private String pgName;

    // Bill Counts
    private Long totalBills;
    private Long pendingBills;
    private Long overdueBills;
    private Long paidBills;

    // Amounts
    private BigDecimal totalBilled;
    private BigDecimal totalCollected;
    private BigDecimal totalPending;
    private BigDecimal totalOverdue;

    // Current Month
    private BigDecimal currentMonthBilled;
    private BigDecimal currentMonthCollected;

    // Pending Actions
    private Long pendingVerifications;
    private Long pendingCheckouts;
}

