package com.pgmanagement.exception;

/**
 * Thrown when image upload limit is exceeded
 * HTTP Status: 400 BAD REQUEST
 */
public class ImageLimitExceededException extends RuntimeException {

    public ImageLimitExceededException(String resourceType, int maxLimit) {
        super(String.format("%s can have maximum %d images", resourceType, maxLimit));
    }

    public ImageLimitExceededException(String message) {
        super(message);
    }
}
