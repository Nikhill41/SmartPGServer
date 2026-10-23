package com.pgmanagement.exception;

/**
 * Thrown when email sending fails (SendGrid)
 * HTTP Status: 500 INTERNAL SERVER ERROR
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
