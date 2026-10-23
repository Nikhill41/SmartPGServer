package com.pgmanagement.service;

import com.pgmanagement.entity.OtpToken;
import com.pgmanagement.enums.OtpPurpose;
import com.pgmanagement.exception.*;
import com.pgmanagement.repository.OtpTokenRepository;
import com.pgmanagement.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for OTP generation, validation, and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final OtpGenerator otpGenerator;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    /**
     * Generate and send OTP to email
     */
    @Transactional
    public void generateAndSendOtp(String email, OtpPurpose purpose) {
        log.info("Generating OTP for email: {} with purpose: {}", email, purpose);

        // Delete any existing OTPs for this email and purpose
        otpTokenRepository.deleteByEmailAndPurpose(email, purpose);

        // Generate new OTP
        String otpCode = otpGenerator.generateOtp();
        String hashedOtp = passwordEncoder.encode(otpCode);

        // Create OTP token
        OtpToken otpToken = new OtpToken();
        otpToken.setEmail(email);
        otpToken.setCodeHash(hashedOtp);
        otpToken.setPurpose(purpose);
        otpToken.setAttemptsLeft(MAX_ATTEMPTS);
        otpToken.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpToken.setVerified(false);

        otpTokenRepository.save(otpToken);

        // Send OTP via email
        emailService.sendOtpEmail(email, otpCode, purpose);

        log.info("OTP generated and sent successfully to: {}", email);
    }

    /**
     * Verify OTP code
     */
    @Transactional
    public boolean verifyOtp(String email, String otpCode, OtpPurpose purpose) {
        log.info("Verifying OTP for email: {} with purpose: {}", email, purpose);

        // Find the latest valid OTP
        Optional<OtpToken> otpTokenOpt = otpTokenRepository
                .findValidOtp(email, purpose, LocalDateTime.now());

        if (otpTokenOpt.isEmpty()) {
            log.warn("No valid OTP found for email: {}", email);
            throw new OtpExpiredException("OTP has expired or does not exist. Please request a new one.");
        }

        OtpToken otpToken = otpTokenOpt.get();

        // Check if attempts exhausted
        if (!otpToken.hasAttemptsLeft()) {
            log.warn("OTP attempts exhausted for email: {}", email);
            throw new OtpAttemptsExhaustedException();
        }

        // Check if OTP has expired
        if (otpToken.isExpired()) {
            log.warn("OTP expired for email: {}", email);
            throw new OtpExpiredException();
        }

        // Verify OTP code
        boolean isValid = passwordEncoder.matches(otpCode, otpToken.getCodeHash());

        if (!isValid) {
            // Decrement attempts
            otpToken.decrementAttempts();
            otpTokenRepository.save(otpToken);

            log.warn("Invalid OTP for email: {}. Attempts left: {}", email, otpToken.getAttemptsLeft());
            throw new OtpInvalidException(otpToken.getAttemptsLeft());
        }

        // Mark as verified
        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);

        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    /**
     * Check if email has verified OTP for given purpose
     */
    public boolean hasVerifiedOtp(String email, OtpPurpose purpose) {
        return otpTokenRepository.hasVerifiedOtp(email, purpose);
    }

    /**
     * Invalidate all OTPs for email and purpose
     */
    @Transactional
    public void invalidateOtps(String email, OtpPurpose purpose) {
        log.info("Invalidating all OTPs for email: {} with purpose: {}", email, purpose);
        otpTokenRepository.deleteByEmailAndPurpose(email, purpose);
    }

    /**
     * Cleanup expired OTP tokens (scheduled task)
     */
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Running OTP cleanup task");
        otpTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Also delete verified tokens older than 24 hours
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        otpTokenRepository.deleteOldVerifiedTokens(cutoffTime);

        log.info("OTP cleanup completed");
    }
}
