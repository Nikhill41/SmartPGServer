package com.pgmanagement.exception;

/**
 * Thrown when OTP has expired
 * HTTP Status: 400 BAD REQUEST
 */
public class OtpExpiredException extends RuntimeException {

    public OtpExpiredException() {
        super("OTP has expired. Please request a new one");
    }

    public OtpExpiredException(String message) {
        super(message);
    }
}
