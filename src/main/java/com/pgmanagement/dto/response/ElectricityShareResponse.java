package com.pgmanagement.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ElectricityShareResponse {

    private Long id;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private BigDecimal totalRoomUnits;
    private Integer activeTenantCount;
    private BigDecimal tenantUnitShare;
    private BigDecimal electricityCharge;
    private BigDecimal sharePercentage;
    private BigDecimal ratePerUnit;
}

