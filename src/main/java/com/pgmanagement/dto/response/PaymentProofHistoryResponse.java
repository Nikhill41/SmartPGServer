package com.pgmanagement.dto.response;



import com.pgmanagement.enums.PaymentMethod;
import com.pgmanagement.enums.PaymentProofStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProofHistoryResponse {

    private Long id;
    private String proofUrl;
    private LocalDateTime uploadedAt;
    private PaymentProofStatus status;
    private String rejectionReason;
    private LocalDateTime verifiedAt;
    private String verifiedByName;
    private Integer attemptNumber;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private LocalDate transactionDate;
    private String notes;
}

