package com.pgmanagement.exception;

/**
 * Thrown when user attempts to create duplicate booking request
 * HTTP Status: 409 CONFLICT
 */
public class DuplicateBookingException extends RuntimeException {

    public DuplicateBookingException() {
        super("You already have a pending booking request for this room");
    }

    public DuplicateBookingException(String message) {
        super(message);
    }
}
