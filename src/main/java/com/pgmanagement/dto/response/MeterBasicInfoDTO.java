package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Basic meter information DTO for list endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeterBasicInfoDTO {

    /**
     * Meter ID
     */
    private String meterId;

    /**
     * Room ID
     */
    private Long roomId;

    /**
     * Room number/name
     */
    private String roomNumber;

    /**
     * PG name
     */
    private String pgName;

    /**
     * Meter status
     */
    private String status;

    /**
     * Online status (optional)
     */
    private Boolean isOnline;

    /**
     * Last seen (optional)
     */
    private String lastSeen;

    /**
     * Paired timestamp
     */
    private LocalDateTime pairedAt;
}
