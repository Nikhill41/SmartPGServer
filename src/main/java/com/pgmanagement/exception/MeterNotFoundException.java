package com.pgmanagement.exception;

/**
 * Thrown when a meter is not found in the database
 * HTTP Status: 404 NOT FOUND
 */
public class MeterNotFoundException extends RuntimeException {

//    public MeterNotFoundException(String message) {
//        super(message);
//    }

    public MeterNotFoundException(String meterId) {
        super(String.format("Meter not found with ID: '%s'", meterId));
    }

    public MeterNotFoundException(String fieldName, Object fieldValue) {
        super(String.format("Meter not found with %s: '%s'", fieldName, fieldValue));
    }
}
