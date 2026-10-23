package com.pgmanagement.dto.response;

import com.pgmanagement.enums.RoomStatus;
import com.pgmanagement.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for room details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long roomId;
    private Long pgId;
    private String pgName;
    private String roomNumber;
    private RoomType type;
    private Integer capacity;
    private Integer currentOccupancy;
    private Integer availableSlots;
    private BigDecimal rent;
    private RoomStatus status;
    private List<String> imageUrls;
    private List<AmenityDTO> amenities;
}
