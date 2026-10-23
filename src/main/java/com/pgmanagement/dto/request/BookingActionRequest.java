package com.pgmanagement.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for owner to reject booking (reason required for rejection)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingActionRequest {

    @Size(min = 10, max = 500, message = "Rejection reason must be between 10 and 500 characters")
    private String rejectionReason;
}
