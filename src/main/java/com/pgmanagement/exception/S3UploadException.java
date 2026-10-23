package com.pgmanagement.exception;

/**
 * Thrown when S3 upload operation fails
 * HTTP Status: 500 INTERNAL SERVER ERROR
 */
public class S3UploadException extends RuntimeException {

    public S3UploadException(String message) {
        super(message);
    }

    public S3UploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
