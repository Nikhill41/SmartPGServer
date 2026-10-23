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
public class UpdateBillingConfigRequest {

    // All fields optional for partial updates

    @Pattern(regexp = "^[a-zA-Z0-9.\\-_]{2,}@[a-zA-Z]{2,}$",
            message = "Invalid UPI ID format")
    private String upiId;

    @Size(max = 100)
    private String upiName;

    @Pattern(regexp = "^[0-9]{9,18}$",
            message = "Bank account number must be 9-18 digits")
    private String bankAccountNumber;

    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
            message = "Invalid IFSC code format")
    private String bankIfscCode;

    @Size(max = 100)
    private String bankAccountName;

    @Size(max = 100)
    private String bankName;

    @Size(max = 100)
    private String bankBranch;

    private Boolean gstEnabled;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
            message = "Invalid GST number format")
    private String gstNumber;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal gstPercentage;

    private BillingMode billingMode;

    @Min(value = 1)
    @Max(value = 28)
    private Integer autoGenerationDay;

    @Min(value = 1)
    @Max(value = 30)
    private Integer dueDaysOffset;

    @DecimalMin(value = "0.01")
    private BigDecimal electricityRatePerUnit;

    private Boolean includeElectricity;
    private ElectricitySplitMethod electricitySplitMethod;

    private Boolean lateFeeEnabled;

    @DecimalMin(value = "0.0")
    private BigDecimal lateFeeAmount;

    @Min(value = 0)
    private Integer lateFeeDays;

    private ProRatedRentMode proRatedRentMode;

    @DecimalMin(value = "0.0")
    private BigDecimal securityDepositAmount;

    @Min(value = 1)
    private Integer securityDepositDueDays;

    private Boolean autoApplyStandardCharges;
    private Boolean proRateStandardCharges;
}

