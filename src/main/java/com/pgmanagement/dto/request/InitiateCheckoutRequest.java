package com.pgmanagement.dto.request;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiateCheckoutRequest {

    @NotNull(message = "Move-out date is required")
    @Future(message = "Move-out date must be in the future")
    private LocalDate moveOutDate;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}

