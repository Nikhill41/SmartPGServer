package com.pgmanagement.dto.request;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementCompleteRequest {

    @NotNull(message = "Settlement date is required")
    @PastOrPresent(message = "Settlement date cannot be in the future")
    private LocalDate settlementDate;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

