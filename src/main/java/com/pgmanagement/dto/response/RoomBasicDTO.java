package com.pgmanagement.dto.response;

import com.pgmanagement.enums.RoomStatus;
import com.pgmanagement.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Basic room info for booking requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomBasicDTO {

    private Long roomId;
    private String roomNumber;
    private RoomType type;
    private Integer capacity;
    private Integer currentOccupancy;
    private BigDecimal rent;
    private RoomStatus status;
}
