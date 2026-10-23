package com.pgmanagement.dto.request;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateStandardChargeRequest {

    @NotNull(message = "PG ID is required")
    @Positive(message = "PG ID must be positive")
    private Long pgId;

    @NotBlank(message = "Charge name is required")
    @Size(min = 2, max = 100, message = "Charge name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s-]+$",
            message = "Charge name can only contain letters, numbers, spaces, and hyphens")
    private String chargeName;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Amount must not exceed 999999.99")
    private BigDecimal amount;

    private Boolean isActive = true;
    private Boolean isDefault = true;
}

