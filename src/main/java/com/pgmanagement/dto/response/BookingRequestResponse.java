package com.pgmanagement.dto.response;

import com.pgmanagement.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for booking request details (shown to owner)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestResponse {

    private Long bookingId;
    private BookingStatus status;
    private LocalDateTime requestDate;
    private LocalDateTime responseDate;
    private String rejectionReason;

    // User details
    private UserBasicInfoDTO user;

    // Room details
    private RoomBasicDTO room;

    // PG details
    private PGBasicDTO pg;
}
