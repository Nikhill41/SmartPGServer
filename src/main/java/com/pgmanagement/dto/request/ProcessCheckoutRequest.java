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
public class ProcessCheckoutRequest {

    @NotNull(message = "Move-out date is required")
    private LocalDate moveOutDate;

    private Boolean deductSecurityDeposit = true;

    private List<AdditionalDeductionRequest> additionalDeductions;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

