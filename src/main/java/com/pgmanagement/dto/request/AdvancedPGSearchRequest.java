package com.pgmanagement.dto.request;

import com.pgmanagement.enums.GenderPreference;
import com.pgmanagement.enums.RoomType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AdvancedPGSearchRequest {

    private String city;
    private GenderPreference genderPreference;
    private RoomType roomType;

    private BigDecimal minRent;
    private BigDecimal maxRent;

    private List<String> amenities;

    // Pagination
    private int page = 0;
    private int size = 10;

    // Sorting
    private String sortBy = "rent"; // rent | createdAt
    private String sortDirection = "ASC"; // ASC | DESC
}

