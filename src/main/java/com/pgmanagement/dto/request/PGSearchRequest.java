package com.pgmanagement.dto.request;

import com.pgmanagement.enums.GenderPreference;
import com.pgmanagement.enums.RoomType;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for searching PGs with filters
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PGSearchRequest {

    private String city;

    private GenderPreference genderPreference;

    private RoomType roomType;

    @DecimalMin(value = "0.0", message = "Minimum rent must be non-negative")
    private BigDecimal minRent = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "Maximum rent must be non-negative")
    private BigDecimal maxRent = new BigDecimal("100000");

    private List<String> amenities;
}
