package com.pgmanagement.dto.request;

import com.pgmanagement.enums.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for updating room (all fields optional)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomRequest {

    @Size(max = 20, message = "Room number must not exceed 20 characters")
    private String roomNumber;

    private RoomType type;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 20, message = "Capacity cannot exceed 20")
    private Integer capacity;

    @DecimalMin(value = "0.0", inclusive = false, message = "Rent must be greater than 0")
    private BigDecimal rent;

    private List<Long> amenityIds;

    @Size(max = 10, message = "Maximum 10 images allowed")
    private List<String> imageUrls;
}
