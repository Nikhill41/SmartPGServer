package com.pgmanagement.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillVersionResponse {

    private Long id;
    private Integer versionNumber;
    private BigDecimal totalAmount;
    private BigDecimal roomRent;
    private BigDecimal electricityCharge;
    private BigDecimal otherCharges;
    private BigDecimal discount;
    private BigDecimal lateFee;
    private BigDecimal gstAmount;
    private String changeReason;
    private String changedByName;
    private LocalDateTime changedAt;
}

