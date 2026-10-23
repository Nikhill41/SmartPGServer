package com.pgmanagement.exception;

/**
 * Thrown when attempting to register with an already existing email
 * HTTP Status: 409 CONFLICT
 */
public class EmailAlreadyExistsException extends RuntimeException {

    // Constructor 1: Takes email and formats message
    public EmailAlreadyExistsException(String email) {
        super(String.format("Email '%s' is already registered", email));
    }
}
