package com.pgmanagement.dto.request;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateBillRequest {

    @NotNull(message = "Tenant ID is required")
    @Positive(message = "Tenant ID must be positive")
    private Long tenantId;

    @NotNull(message = "Room ID is required")
    @Positive(message = "Room ID must be positive")
    private Long roomId;

    @NotNull(message = "Billing period start date is required")
    @PastOrPresent(message = "Billing period start must not be in the future")
    private LocalDate billingPeriodStart;

    @NotNull(message = "Billing period end date is required")
    @PastOrPresent(message = "Billing period end must not be in the future")
    private LocalDate billingPeriodEnd;

    @DecimalMin(value = "0.0", message = "Discount cannot be negative")
    private BigDecimal discount = BigDecimal.ZERO;

    private List<BillChargeRequest> additionalCharges;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    // ✅ NEW: Optional manual meter readings
    // If provided, DynamoDB fetch is skipped and these values are used directly
    @DecimalMin(value = "0.0", message = "Previous reading cannot be negative")
    private BigDecimal previousReading;

    @DecimalMin(value = "0.0", message = "Current reading cannot be negative")
    private BigDecimal currentReading;

    @DecimalMin(value = "0.0", message = "Units consumed cannot be negative")
    private BigDecimal manualUnitsConsumed;

    // Helper: check if owner provided manual readings
    public boolean hasManualReadings() {
        return manualUnitsConsumed != null && manualUnitsConsumed.compareTo(BigDecimal.ZERO) >= 0;
    }
}

