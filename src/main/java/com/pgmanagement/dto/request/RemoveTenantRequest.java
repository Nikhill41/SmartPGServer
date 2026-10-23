package com.pgmanagement.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemoveTenantRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Size(max = 500, message = "Removal reason cannot exceed 500 characters")
    private String removalReason;
}
