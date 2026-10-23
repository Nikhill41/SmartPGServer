package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for amenity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmenityDTO {

    private Long id;
    private String name;
    private String icon;
    private Boolean isPredefined;
}
