package com.pgmanagement.dto.response;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkBillGenerationResponse {

    private Integer totalTenants;
    private Integer successCount;
    private Integer failureCount;
    private List<BillResponse> generatedBills;
    private List<BillGenerationError> errors;
}

