package com.pgmanagement.dto.request;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkBillGenerationRequest {

    @NotNull(message = "PG ID is required")
    @Positive(message = "PG ID must be positive")
    private Long pgId;

    @NotNull(message = "Billing period start date is required")
    @PastOrPresent(message = "Billing period start must not be in the future")
    private LocalDate billingPeriodStart;

    @NotNull(message = "Billing period end date is required")
    @PastOrPresent(message = "Billing period end must not be in the future")
    private LocalDate billingPeriodEnd;
}

