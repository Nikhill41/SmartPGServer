package com.pgmanagement.exception;

/**
 * Thrown when OTP verification fails
 * HTTP Status: 400 BAD REQUEST
 */
public class OtpInvalidException extends RuntimeException {

    public OtpInvalidException() {
        super("Invalid OTP code");
    }

    public OtpInvalidException(String message) {
        super(message);
    }

    public OtpInvalidException(int attemptsLeft) {
        super(String.format("Invalid OTP code. %d attempts remaining", attemptsLeft));
    }
}
