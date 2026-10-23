package com.pgmanagement.exception;

/**
 * Thrown when login credentials are invalid
 * HTTP Status: 401 UNAUTHORIZED
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
