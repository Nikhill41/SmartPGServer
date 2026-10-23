package com.pgmanagement.dto.response;

import com.pgmanagement.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Basic user info shown to owners in booking requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfoDTO {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Gender gender;
    private Boolean profileVerified;
    private Boolean isActive;
    private LocalDateTime accountCreatedDate;
    private Boolean hasActiveBooking;
}
