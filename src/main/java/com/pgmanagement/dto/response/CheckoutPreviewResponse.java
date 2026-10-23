package com.pgmanagement.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutPreviewResponse {

    private Long tenantId;
    private String tenantName;
    private Long roomId;
    private String roomNumber;
    private LocalDate moveInDate;
    private LocalDate intendedMoveOutDate;
    private Integer daysStayed;

    // Charges
    private BigDecimal proRatedRent;
    private BigDecimal electricityCharge;
    private BigDecimal otherCharges;
    private BigDecimal subtotal;
    private BigDecimal gstAmount;
    private BigDecimal totalDues;

    // Security Deposit
    private BigDecimal securityDepositPaid;
    private BigDecimal securityDepositUsed;
    private BigDecimal securityDepositRefund;

    // Pending Bills
    private List<BillResponse> pendingBills;
    private BigDecimal pendingBillsAmount;

    // Final Amount
    private BigDecimal finalAmountDue;
    private BigDecimal finalAmountToRefund;
}

