package com.pgmanagement.dto.request;



import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationRequest {


    @Pattern(regexp = "^(APPROVE|REJECT)$", message = "Action must be APPROVE or REJECT")
    private String action;

    @PastOrPresent(message = "Paid date cannot be in the future")
    private LocalDate paidDate;

    @Size(min = 10, max = 500, message = "Rejection reason must be between 10 and 500 characters")
    private String rejectionReason;

    @Size(max = 500, message = "Verification notes must not exceed 500 characters")
    private String verificationNotes;
}
