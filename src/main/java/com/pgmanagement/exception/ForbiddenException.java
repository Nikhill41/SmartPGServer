package com.pgmanagement.exception;

/**
 * Thrown when user is authenticated but doesn't have permission
 * HTTP Status: 403 FORBIDDEN
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException() {
        super("You don't have permission to perform this action");
    }
}
