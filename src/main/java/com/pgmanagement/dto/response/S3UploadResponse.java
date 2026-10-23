package com.pgmanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for S3 upload operations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class S3UploadResponse {

    private String fileUrl;
    private String fileName;
    private String message;
}
