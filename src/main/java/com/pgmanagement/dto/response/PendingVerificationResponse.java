package com.pgmanagement.dto.response;



import com.pgmanagement.enums.PaymentMethod;
import com.pgmanagement.enums.PaymentProofStatus;
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
public class PendingVerificationResponse {
    private Long billId;
    private String billNumber;
    private Long tenantId;
    private String tenantName;
    private String tenantEmail;
    private BigDecimal totalAmount;
    private String paymentProofUrl;
    private PaymentProofStatus paymentProofStatus;
    private LocalDateTime paymentProofUploadedAt;
    private String transactionId;
    private PaymentMethod paymentMethod;
    private Integer rejectionCount;
    private String lastRejectionReason;
}

