package com.pgmanagement.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EditBillRequest {

    @DecimalMin(value = "0.0", message = "Room rent cannot be negative")
    private BigDecimal roomRent;

    @DecimalMin(value = "0.0", message = "Electricity charge cannot be negative")
    private BigDecimal electricityCharge;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private BigDecimal discount;

    @DecimalMin(value = "0.0", message = "Late fee cannot be negative")
    private BigDecimal lateFee;

    @DecimalMin(value = "0.0", message = "Additional deductions cannot be negative")
    private BigDecimal additionalDeductions;  // ✅ ADD THIS

    private List<BillChargeRequest> standardCharges;

    @NotBlank(message = "Edit reason is required")
    @Size(min = 10, max = 500, message = "Edit reason must be between 10 and 500 characters")
    private String editReason;
}
