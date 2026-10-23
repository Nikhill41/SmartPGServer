package com.pgmanagement.dto.response;



import com.pgmanagement.enums.BillingMode;
import com.pgmanagement.enums.ElectricitySplitMethod;
import com.pgmanagement.enums.ProRatedRentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingConfigResponse {

    private Long id;
    private Long pgId;
    private String pgName;

    // Payment Accounts
    private String upiId;
    private String upiName;
    private String bankAccountNumber;
    private String bankIfscCode;
    private String bankAccountName;
    private String bankName;
    private String bankBranch;

    // GST
    private Boolean gstEnabled;
    private String gstNumber;
    private BigDecimal gstPercentage;

    // Billing Mode
    private BillingMode billingMode;
    private Integer autoGenerationDay;
    private Integer dueDaysOffset;

    // Electricity
    private BigDecimal electricityRatePerUnit;
    private Boolean includeElectricity;
    private ElectricitySplitMethod electricitySplitMethod;

    // Late Fees
    private Boolean lateFeeEnabled;
    private BigDecimal lateFeeAmount;
    private Integer lateFeeDays;

    // Pro-rated Rent
    private ProRatedRentMode proRatedRentMode;

    // Security Deposit
    private BigDecimal securityDepositAmount;
    private Integer securityDepositDueDays;

    // Standard Charges
    private Boolean autoApplyStandardCharges;
    private Boolean proRateStandardCharges;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

