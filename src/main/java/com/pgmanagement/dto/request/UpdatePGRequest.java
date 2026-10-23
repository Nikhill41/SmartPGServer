package com.pgmanagement.dto.request;

import com.pgmanagement.enums.GenderPreference;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating PG (all fields optional)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePGRequest {

    @Size(min = 3, max = 200, message = "PG name must be between 3 and 200 characters")
    private String name;

    private String address;

    @Size(min = 2, max = 100, message = "City name must be between 2 and 100 characters")
    private String city;

    private String state;

    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    private String description;

    private GenderPreference genderPreference;

    @Size(max = 5, message = "Maximum 5 images allowed")
    private List<String> imageUrls;
}
