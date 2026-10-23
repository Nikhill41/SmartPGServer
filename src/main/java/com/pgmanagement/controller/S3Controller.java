package com.pgmanagement.controller;

import com.pgmanagement.dto.response.ApiResponse;
import com.pgmanagement.dto.response.PresignedUrlResponse;
import com.pgmanagement.dto.response.S3UploadResponse;
import com.pgmanagement.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 Controller
 * Handles: Image upload (presigned URL + server-side upload)
 */
@RestController
@RequestMapping("/api/s3")
@RequiredArgsConstructor
@Slf4j
public class S3Controller {

    private final S3Service s3Service;

    /**
     * Generate presigned URL for client-side upload
     */
    @GetMapping("/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam String folder,
            @RequestParam String fileName,
            @RequestParam String contentType) {

        log.info("Generating presigned URL for folder: {}, file: {}", folder, fileName);
        PresignedUrlResponse response = s3Service.generatePresignedUploadUrl(folder, fileName, contentType);

        return ResponseEntity.ok(
                ApiResponse.success("Presigned URL generated successfully", response)
        );
    }

    /**
     * Server-side file upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<S3UploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam String folder) {

        log.info("Uploading file to folder: {}, filename: {}", folder, file.getOriginalFilename());
        S3UploadResponse response = s3Service.uploadFile(file, folder);

        return ResponseEntity.ok(
                ApiResponse.success("File uploaded successfully", response)
        );
    }

    /**
     * Delete file from S3
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam String fileUrl) {
        log.info("Deleting file: {}", fileUrl);
        s3Service.deleteFile(fileUrl);

        return ResponseEntity.ok(
                ApiResponse.success("File deleted successfully")
        );
    }
}
