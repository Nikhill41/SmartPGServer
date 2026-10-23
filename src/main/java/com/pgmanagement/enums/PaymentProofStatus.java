package com.pgmanagement.enums;

public enum PaymentProofStatus {
    UPLOADED,    // Tenant uploaded proof, awaiting verification
    VERIFIED,    // Owner verified and approved
    REJECTED     // Owner rejected, tenant needs to re-upload
}