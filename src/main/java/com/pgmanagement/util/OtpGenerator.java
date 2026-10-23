package com.pgmanagement.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Utility class for generating secure OTP codes
 */
@Component
public class OtpGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int OTP_LENGTH = 6;

    /**
     * Generate a secure 6-digit OTP
     */
    public String generateOtp() {
        int otp = 100000 + secureRandom.nextInt(900000); // Range: 100000-999999
        return String.valueOf(otp);
    }

    /**
     * Generate OTP with custom length
     */
    public String generateOtp(int length) {
        if (length < 4 || length > 10) {
            throw new IllegalArgumentException("OTP length must be between 4 and 10");
        }

        int min = (int) Math.pow(10, length - 1);
        int max = (int) Math.pow(10, length) - 1;
        int otp = min + secureRandom.nextInt(max - min + 1);

        return String.valueOf(otp);
    }
}
