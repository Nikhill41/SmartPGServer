package com.pgmanagement.exception;

/**
 * Thrown when user attempts to access a meter they don't have permission for
 * HTTP Status: 403 FORBIDDEN
 */
public class UnauthorizedMeterAccessException extends RuntimeException {

    public UnauthorizedMeterAccessException(String message) {
        super(message);
    }

    public UnauthorizedMeterAccessException() {
        super("You don't have permission to access this meter");
    }

    public UnauthorizedMeterAccessException(String meterId, Long userId) {
        super(String.format("User %d is not authorized to access meter '%s'", userId, meterId));
    }
}
