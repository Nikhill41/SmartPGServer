package com.pgmanagement.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillChargeResponse {

    private Long id;
    private String chargeName;
    private BigDecimal amount;
    private Boolean isProRated;
}

