package com.pgmanagement.dto.response;



import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.BillType;
import com.pgmanagement.enums.GenerationType;
import com.pgmanagement.enums.PaymentProofStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillResponse {

    private Long id;
    private String billNumber;
    private Long ownerId;
    // References
    private Long tenantId;
    private String tenantName;
    private String tenantEmail;

    private Long roomId;
    private String roomNumber;

    private Long pgId;
    private String pgName;

    // Billing Period
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    // Charges
    private BigDecimal roomRent;
    private BigDecimal electricityCharge;
    private BigDecimal otherCharges;
    private BigDecimal discount;
    private BigDecimal lateFee;
    private BigDecimal additionalDeductions;

    // GST
    private BigDecimal subtotal;
    private Boolean gstEnabled;
    private BigDecimal gstPercentage;
    private BigDecimal gstAmount;

    // Security Deposit
    private BigDecimal securityDepositUsed;

    // Total
    private BigDecimal totalAmount;

    // Electricity
    private BigDecimal totalRoomUnits;
    private BigDecimal tenantUnitShare;
    private Integer totalRoomTenants;

    // Status
    private BillStatus status;
    private BillType billType;
    private GenerationType generationType;

    // Dates
    private LocalDate dueDate;
    private LocalDate paidDate;
    private LocalDate generatedDate;

    // Payment Proof
    private String paymentProofUrl;
    private LocalDateTime paymentProofUploadedAt;
    private PaymentProofStatus paymentProofStatus;
    private Integer paymentRejectionCount;
    private String lastRejectionReason;

    // Versioning
    private Integer version;

    // Notes
    private String notes;

    // Payment Links
    private String upiPaymentLink;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
