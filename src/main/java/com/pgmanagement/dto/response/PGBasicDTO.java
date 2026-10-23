package com.pgmanagement.dto.response;

import com.pgmanagement.enums.GenderPreference;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basic PG info for booking requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PGBasicDTO {

    private Long pgId;
    private String pgName;
    private String address;
    private String city;
    private GenderPreference genderPreference;
}
