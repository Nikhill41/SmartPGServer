package com.pgmanagement.dto.request;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalDeductionRequest {

    @NotBlank(message = "Deduction name is required")
    @Size(max = 100, message = "Deduction name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    private BigDecimal amount;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}

