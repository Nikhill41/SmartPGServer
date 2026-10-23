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
public class BillDetailsResponse {

    private BillResponse bill;
    private List<BillChargeResponse> charges;
    private List<BillVersionResponse> versionHistory;
    private List<PaymentProofHistoryResponse> paymentHistory;
    private ElectricityShareResponse electricityShare;
}

