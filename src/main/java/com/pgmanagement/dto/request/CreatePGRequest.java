package com.pgmanagement.dto.request;

import com.pgmanagement.enums.GenderPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO for creating PG
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatePGRequest {

    @NotBlank(message = "PG name is required")
    @Size(min = 3, max = 200, message = "PG name must be between 3 and 200 characters")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    private String description;

    @NotNull(message = "Gender preference is required")
    private GenderPreference genderPreference;

    @Size(max = 5, message = "Maximum 5 images allowed")
    private List<String> imageUrls = new ArrayList<>();
}
