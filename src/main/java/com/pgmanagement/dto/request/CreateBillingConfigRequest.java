//package com.pgmanagement.dto.request;
//
//
//
//import com.pgmanagement.enums.BillingMode;
//import com.pgmanagement.enums.ElectricitySplitMethod;
//import com.pgmanagement.enums.ProRatedRentMode;
//import jakarta.validation.constraints.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class CreateBillingConfigRequest {
//
//    @NotNull(message = "PG ID is required")
//    @Positive(message = "PG ID must be positive")
//    private Long pgId;
//
//    // Payment Accounts
//    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,}@[a-zA-Z]{2,}$",
//            message = "Invalid UPI ID format")
//    private String upiId;
//
//    @Size(max = 100, message = "UPI name must not exceed 100 characters")
//    private String upiName;
//
//    @Pattern(regexp = "^[0-9]{9,18}$",
//            message = "Bank account number must be 9-18 digits")
//    private String bankAccountNumber;
//
//    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
//            message = "Invalid IFSC code format")
//    private String bankIfscCode;
//
//    @Size(max = 100, message = "Bank account name must not exceed 100 characters")
//    private String bankAccountName;
//
//    @Size(max = 100, message = "Bank name must not exceed 100 characters")
//    private String bankName;
//
//    @Size(max = 100, message = "Bank branch must not exceed 100 characters")
//    private String bankBranch;
//
//    // GST Configuration
//    private Boolean gstEnabled = false;
//
//    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
//            message = "Invalid GST number format")
//    private String gstNumber;
//
//    @DecimalMin(value = "0.0", message = "GST percentage must be between 0 and 100")
//    @DecimalMax(value = "100.0", message = "GST percentage must be between 0 and 100")
//    private BigDecimal gstPercentage = BigDecimal.ZERO;
//
//    // Billing Mode
//    @NotNull(message = "Billing mode is required")
//    private BillingMode billingMode = BillingMode.MANUAL;
//
//    @Min(value = 1, message = "Auto generation day must be between 1 and 28")
//    @Max(value = 28, message = "Auto generation day must be between 1 and 28")
//    private Integer autoGenerationDay = 1;
//
//    @Min(value = 1, message = "Due days offset must be at least 1")
//    @Max(value = 30, message = "Due days offset must not exceed 30")
//    private Integer dueDaysOffset = 7;
//
//    // Electricity
//    @DecimalMin(value = "0.01", message = "Electricity rate must be positive")
//    private BigDecimal electricityRatePerUnit = new BigDecimal("8.5");
//
//    private Boolean includeElectricity = true;
//
//    private ElectricitySplitMethod electricitySplitMethod = ElectricitySplitMethod.TIME_WEIGHTED;
//
//    // Late Fees
//    private Boolean lateFeeEnabled = false;
//
//    @DecimalMin(value = "0.0", message = "Late fee amount cannot be negative")
//    private BigDecimal lateFeeAmount = BigDecimal.ZERO;
//
//    @Min(value = 0, message = "Late fee days cannot be negative")
//    private Integer lateFeeDays = 3;
//
//    // Pro-rated Rent
//    @NotNull(message = "Pro-rated rent mode is required")
//    private ProRatedRentMode proRatedRentMode = ProRatedRentMode.DAILY;
//
//    // Security Deposit
//    @DecimalMin(value = "0.0", message = "Security deposit amount cannot be negative")
//    private BigDecimal securityDepositAmount = new BigDecimal("10000");
//
//    @Min(value = 1, message = "Security deposit due days must be at least 1")
//    private Integer securityDepositDueDays = 7;
//
//    // Standard Charges
//    private Boolean autoApplyStandardCharges = true;
//    private Boolean proRateStandardCharges = false;
//}



package com.pgmanagement.dto.request;

import com.pgmanagement.enums.BillingMode;
import com.pgmanagement.enums.ElectricitySplitMethod;
import com.pgmanagement.enums.ProRatedRentMode;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBillingConfigRequest {

    @NotNull(message = "PG ID is required")
    @Positive(message = "PG ID must be positive")
    private Long pgId;

    // ================= PAYMENT ACCOUNTS =================

    // Optional but must match format if provided
    @Pattern(
            regexp = "(^$|^[a-zA-Z0-9.\\-_]{2,}@[a-zA-Z]{2,}$)",
            message = "Invalid UPI ID format"
    )
    private String upiId;

    @Size(max = 100, message = "UPI name must not exceed 100 characters")
    private String upiName;

    @Pattern(
            regexp = "(^$|^[0-9]{9,18}$)",
            message = "Bank account number must be 9-18 digits"
    )
    private String bankAccountNumber;

    @Pattern(
            regexp = "(^$|^[A-Z]{4}0[A-Z0-9]{6}$)",
            message = "Invalid IFSC code format"
    )
    private String bankIfscCode;

    @Size(max = 100, message = "Bank account name must not exceed 100 characters")
    private String bankAccountName;

    @Size(max = 100, message = "Bank name must not exceed 100 characters")
    private String bankName;

    @Size(max = 100, message = "Bank branch must not exceed 100 characters")
    private String bankBranch;

    // ================= GST CONFIG =================

    private Boolean gstEnabled = false;

    // Optional but must match format if provided
    @Pattern(
            regexp = "(^$|^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$)",
            message = "Invalid GST number format"
    )
    private String gstNumber;

    @DecimalMin(value = "0.0", message = "GST percentage must be between 0 and 100")
    @DecimalMax(value = "100.0", message = "GST percentage must be between 0 and 100")
    private BigDecimal gstPercentage = BigDecimal.ZERO;

    // ================= BILLING MODE =================

    @NotNull(message = "Billing mode is required")
    private BillingMode billingMode = BillingMode.MANUAL;

    @Min(value = 1, message = "Auto generation day must be between 1 and 28")
    @Max(value = 28, message = "Auto generation day must be between 1 and 28")
    private Integer autoGenerationDay = 1;

    @Min(value = 1, message = "Due days offset must be at least 1")
    @Max(value = 30, message = "Due days offset must not exceed 30")
    private Integer dueDaysOffset = 7;

    // ================= ELECTRICITY =================

    @DecimalMin(value = "0.01", message = "Electricity rate must be positive")
    private BigDecimal electricityRatePerUnit = new BigDecimal("8.5");

    private Boolean includeElectricity = true;

    private ElectricitySplitMethod electricitySplitMethod =
            ElectricitySplitMethod.TIME_WEIGHTED;

    // ================= LATE FEES =================

    private Boolean lateFeeEnabled = false;

    @DecimalMin(value = "0.0", message = "Late fee amount cannot be negative")
    private BigDecimal lateFeeAmount = BigDecimal.ZERO;

    @Min(value = 0, message = "Late fee days cannot be negative")
    private Integer lateFeeDays = 3;

    // ================= PRO-RATED RENT =================

    @NotNull(message = "Pro-rated rent mode is required")
    private ProRatedRentMode proRatedRentMode =
            ProRatedRentMode.DAILY;

    // ================= SECURITY DEPOSIT =================

    @DecimalMin(value = "0.0", message = "Security deposit amount cannot be negative")
    private BigDecimal securityDepositAmount =
            new BigDecimal("10000");

    @Min(value = 1, message = "Security deposit due days must be at least 1")
    private Integer securityDepositDueDays = 7;

    // ================= STANDARD CHARGES =================

    private Boolean autoApplyStandardCharges = true;
    private Boolean proRateStandardCharges = false;
}
