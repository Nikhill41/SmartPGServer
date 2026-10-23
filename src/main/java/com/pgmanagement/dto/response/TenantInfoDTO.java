package com.pgmanagement.dto.response;

import com.pgmanagement.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfoDTO {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private Gender gender;
    private LocalDateTime moveInDate;
    private Long daysStayed;
    private Long roomId;
    private String roomNumber;
}
