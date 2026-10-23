package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterPreviewResponse {
    private boolean hasMeter;
    private String meterId;
    private BigDecimal previousReading;  // kWh at period start
    private BigDecimal currentReading;   // kWh at period end
    private BigDecimal unitsConsumed;    // currentReading - previousReading
    private String message;              // e.g. "No meter found" or "No data available"
}

