package com.pgmanagement.exception;

/**
 * Thrown when attempting to book a room that's not available
 * HTTP Status: 409 CONFLICT
 */
public class RoomNotAvailableException extends RuntimeException {

    public RoomNotAvailableException() {
        super("Room is not available for booking");
    }

    public RoomNotAvailableException(String message) {
        super(message);
    }
}
