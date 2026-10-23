package com.pgmanagement.exception;

/**
 * Thrown when attempting invalid booking status transition
 * HTTP Status: 400 BAD REQUEST
 */
public class InvalidBookingStatusException extends RuntimeException {

    public InvalidBookingStatusException(String message) {
        super(message);
    }

    public InvalidBookingStatusException(String currentStatus, String attemptedAction) {
        super(String.format("Cannot %s booking with status: %s", attemptedAction, currentStatus));
    }
}
