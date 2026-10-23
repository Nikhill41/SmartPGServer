package com.pgmanagement.exception;

/**
 * Thrown when maximum OTP verification attempts exceeded
 * HTTP Status: 429 TOO MANY REQUESTS
 */
public class OtpAttemptsExhaustedException extends RuntimeException {

    public OtpAttemptsExhaustedException() {
        super("Maximum OTP verification attempts exceeded. Please request a new OTP");
    }

    public OtpAttemptsExhaustedException(String message) {
        super(message);
    }
}
