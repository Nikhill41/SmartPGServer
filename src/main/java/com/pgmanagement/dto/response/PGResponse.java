package com.pgmanagement.dto.response;

import com.pgmanagement.enums.GenderPreference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for PG details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PGResponse {

    private Long pgId;
    private Long ownerId;
    private String ownerName;
    private String ownerPhone;
    private String name;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private String description;
    private GenderPreference genderPreference;
    private List<String> imageUrls;
    private Integer totalRooms;
    private Integer availableRooms;
    private LocalDateTime createdAt;
}
