package com.pgmanagement.service;

import com.pgmanagement.dto.response.PresignedUrlResponse;
import com.pgmanagement.dto.response.S3UploadResponse;
import com.pgmanagement.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest; // ✅ NEW
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest; // ✅ NEW

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private static final long PRESIGNED_URL_EXPIRY_MINUTES = 15;
    private static final long PRESIGNED_GET_URL_EXPIRY_HOURS = 24;

    // ========== EXISTING METHODS (UNCHANGED) ==========

    /**
     * Generate presigned URL for client-side upload
     */
    public PresignedUrlResponse generatePresignedUploadUrl(String folder, String fileName, String contentType) {
        try {
            String fileKey = generateFileKey(folder, fileName);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(PRESIGNED_URL_EXPIRY_MINUTES))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

            String presignedUrl = presignedRequest.url().toString();

            log.info("Generated presigned URL for file: {}", fileKey);

            return new PresignedUrlResponse(
                    presignedUrl,
                    fileKey,
                    PRESIGNED_URL_EXPIRY_MINUTES * 60,
                    "Upload file to this URL using PUT request"
            );

        } catch (Exception e) {
            log.error("Error generating presigned URL", e);
            throw new S3UploadException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Server-side upload of file to S3
     */
    public S3UploadResponse uploadFile(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileKey = generateFileKey(folder, originalFilename);

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = getFileUrl(fileKey);

            log.info("File uploaded successfully: {}", fileKey);

            return new S3UploadResponse(
                    fileUrl,
                    fileKey,
                    "File uploaded successfully"
            );

        } catch (IOException e) {
            log.error("Error uploading file to S3", e);
            throw new S3UploadException("Failed to upload file", e);
        }
    }

    /**
     * Delete file from S3
     */
    public void deleteFile(String fileUrl) {
        try {
            String fileKey = extractFileKeyFromUrl(fileUrl);

            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);

            log.info("File deleted successfully: {}", fileKey);

        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", fileUrl, e);
            throw new S3UploadException("Failed to delete file", e);
        }
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String fileUrl) {
        try {
            String fileKey = extractFileKeyFromUrl(fileUrl);

            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence: {}", fileUrl, e);
            return false;
        }
    }

    // ========== ✅ NEW METHODS FOR PRESIGNED GET URLs ==========

    /**
     * Generate presigned GET URL for viewing a single image (24-hour expiry)
     */
    /**
     * Generate presigned GET URL for viewing a single image (24-hour expiry)
     * ✅ FIXED: Check if URL is already presigned to avoid double-signing
     */
    public String generatePresignedGetUrl(String fileUrl) {
        try {
            // ✅ CHECK: If URL already contains presigned parameters, return as-is
            if (fileUrl.contains("X-Amz-Algorithm") || fileUrl.contains("X-Amz-Signature")) {
                log.debug("URL is already presigned, returning as-is: {}", fileUrl);
                return fileUrl;
            }

            String fileKey = extractFileKeyFromUrl(fileUrl);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(PRESIGNED_GET_URL_EXPIRY_HOURS))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

            String presignedUrl = presignedRequest.url().toString();

            log.debug("Generated presigned GET URL for: {}", fileKey);

            return presignedUrl;

        } catch (Exception e) {
            log.error("Error generating presigned GET URL for: {}", fileUrl, e);
            // Return original URL as fallback
            return fileUrl;
        }
    }


    /**
     * Convert list of S3 URLs to presigned GET URLs
     */
    public List<String> generatePresignedGetUrls(List<String> fileUrls) {
        if (fileUrls == null || fileUrls.isEmpty()) {
            return List.of();
        }

        return fileUrls.stream()
                .map(this::generatePresignedGetUrl)
                .collect(Collectors.toList());
    }

    // ========== EXISTING PRIVATE METHODS (UNCHANGED) ==========

    /**
     * Generate unique file key with folder structure
     */
    private String generateFileKey(String folder, String originalFilename) {
        String uniqueId = UUID.randomUUID().toString();
        String extension = getFileExtension(originalFilename);
        return String.format("%s/%s%s", folder, uniqueId, extension);
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Build public file URL
     */
    private String getFileUrl(String fileKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName, region, fileKey);
    }

    /**
     * Extract file key from full S3 URL
     */
    private String extractFileKeyFromUrl(String fileUrl) {
        // URL format: https://bucket-name.s3.region.amazonaws.com/folder/file.jpg
        String[] parts = fileUrl.split(bucketName + ".s3." + region + ".amazonaws.com/");
        if (parts.length == 2) {
            return parts[1];
        }
        throw new IllegalArgumentException("Invalid S3 URL format: " + fileUrl);
    }



    /**
     * ✅ NEW: Upload payment proof file with custom key
     * Used by PaymentVerificationService
     */
    public String uploadCustomKeyFile(MultipartFile file, String customKey) throws IOException {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(customKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            String fileUrl = getFileUrl(customKey);
            log.info("Payment proof uploaded successfully: {}", customKey);

            return fileUrl;

        } catch (IOException e) {
            log.error("Error uploading payment proof to S3", e);
            throw new S3UploadException("Failed to upload payment proof", e);
        }
    }

}
