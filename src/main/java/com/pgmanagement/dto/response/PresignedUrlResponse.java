package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for presigned URL generation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlResponse {

    private String presignedUrl;
    private String fileKey;
    private Long expiresIn; // seconds
    private String message;
}
