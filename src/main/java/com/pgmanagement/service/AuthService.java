package com.pgmanagement.service;

import com.pgmanagement.dto.request.*;
import com.pgmanagement.dto.response.AuthResponse;
import com.pgmanagement.entity.Owner;
import com.pgmanagement.entity.User;
import com.pgmanagement.enums.OtpPurpose;
import com.pgmanagement.enums.Role;
import com.pgmanagement.exception.*;
import com.pgmanagement.repository.OwnerRepository;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.IllegalStateException;

/**
 * Service for authentication operations
 * NEW FLOW: Register (unverified) → Verify OTP → Auto-login
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * OWNER REGISTRATION (Creates unverified account + sends OTP)
     */
    @Transactional
    public void registerOwner(OwnerRegistrationRequest request) {
        log.info("Owner registration initiated for email: {}", request.getEmail());

        // Check if email exists
        Owner existingOwner = ownerRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingOwner != null) {
            // If already verified → Error
            if (existingOwner.getIsVerified()) {
                throw new EmailAlreadyExistsException("Email already registered. Please login.");
            }

            // If NOT verified → Delete old record (fresh start)
            log.info("Deleting old unverified account for email: {}", request.getEmail());
            ownerRepository.delete(existingOwner);

            // Also cleanup old OTP
            otpService.invalidateOtps(request.getEmail(), OtpPurpose.OWNER_REGISTRATION);
        }

        // Create new UNVERIFIED owner account
        Owner owner = new Owner();
        owner.setName(request.getName());
        owner.setEmail(request.getEmail());
        owner.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        owner.setPhone(request.getPhone());
        owner.setRole(Role.OWNER);
        owner.setIsVerified(false); // ← KEY: Unverified

        ownerRepository.save(owner);

        // Send OTP
        otpService.generateAndSendOtp(request.getEmail(), OtpPurpose.OWNER_REGISTRATION);

        log.info("Owner account created (unverified) and OTP sent: {}", request.getEmail());
    }

    /**
     * USER REGISTRATION (Creates unverified account + sends OTP)
     */
    @Transactional
    public void registerUser(UserRegistrationRequest request) {
        log.info("User registration initiated for email: {}", request.getEmail());

        // Check if email exists
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingUser != null) {
            // If already verified → Error
            if (existingUser.getIsVerified()) {
                throw new EmailAlreadyExistsException("Email already registered. Please login.");
            }

            // If NOT verified → Delete old record (fresh start)
            log.info("Deleting old unverified account for email: {}", request.getEmail());
            userRepository.delete(existingUser);

            // Also cleanup old OTP
            otpService.invalidateOtps(request.getEmail(), OtpPurpose.USER_REGISTRATION);
        }

        // Create new UNVERIFIED user account
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setRole(Role.USER);
        user.setIsVerified(false); // ← KEY: Unverified
        user.setIsActive(true);

        userRepository.save(user);

        // Send OTP
        otpService.generateAndSendOtp(request.getEmail(), OtpPurpose.USER_REGISTRATION);

        log.info("User account created (unverified) and OTP sent: {}", request.getEmail());
    }

    /**
     * VERIFY OTP (Marks account as verified + returns JWT for auto-login)
     */
    @Transactional
    public AuthResponse verifyRegistrationOtp(OtpVerificationRequest request) {
        log.info("Verifying OTP for email: {} with purpose: {}", request.getEmail(), request.getPurpose());

        // Verify OTP first
        otpService.verifyOtp(request.getEmail(), request.getOtp(), request.getPurpose());

        // Based on purpose, verify owner or user
        if (request.getPurpose() == OtpPurpose.OWNER_REGISTRATION) {
            return verifyOwnerAccount(request.getEmail());
        } else if (request.getPurpose() == OtpPurpose.USER_REGISTRATION) {
            return verifyUserAccount(request.getEmail());
        } else {
            throw new IllegalArgumentException("Invalid OTP purpose for registration verification");
        }
    }

    /**
     * Verify owner account and return JWT
     */
    private AuthResponse verifyOwnerAccount(String email) {
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "email", email));

        // Mark as verified
        owner.setIsVerified(true);
        ownerRepository.save(owner);

        // Cleanup OTP
        otpService.invalidateOtps(email, OtpPurpose.OWNER_REGISTRATION);

        // Generate JWT token (auto-login)
        String token = jwtTokenProvider.generateToken(owner.getId(), owner.getEmail(), owner.getRole());

        log.info("Owner account verified successfully: {}", email);

        return new AuthResponse(token, owner.getId(), owner.getName(), owner.getEmail(), owner.getRole());
    }

    /**
     * Verify user account and return JWT
     */
    private AuthResponse verifyUserAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Mark as verified
        user.setIsVerified(true);
        userRepository.save(user);

        // Cleanup OTP
        otpService.invalidateOtps(email, OtpPurpose.USER_REGISTRATION);

        // Generate JWT token (auto-login)
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

        log.info("User account verified successfully: {}", email);

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    /**
     * LOGIN (works for both Owner and User)
     * ONLY allows verified accounts
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Try to find as Owner first
        Owner owner = ownerRepository.findByEmail(request.getEmail()).orElse(null);
        if (owner != null) {
            // Check if verified
            if (!owner.getIsVerified()) {
                throw new UnauthorizedException("Please verify your email first. Check your inbox for OTP.");
            }

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), owner.getPasswordHash())) {
                throw new InvalidCredentialsException();
            }

            // Generate token
            String token = jwtTokenProvider.generateToken(owner.getId(), owner.getEmail(), owner.getRole());

            log.info("Owner login successful: {}", request.getEmail());
            return new AuthResponse(token, owner.getId(), owner.getName(), owner.getEmail(), owner.getRole());
        }

        // Try to find as User
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user != null) {
            // Check if verified
            if (!user.getIsVerified()) {
                throw new UnauthorizedException("Please verify your email first. Check your inbox for OTP.");
            }

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new InvalidCredentialsException();
            }

            // Generate token
            String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());

            log.info("User login successful: {}", request.getEmail());
            return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
        }

        // No account found
        throw new InvalidCredentialsException();
    }

    /**
     * RESEND OTP
     */
    @Transactional
    public void resendOtp(ResendOtpRequest request) {
        log.info("Resending OTP for email: {} with purpose: {}", request.getEmail(), request.getPurpose());

        // Check if account exists and is unverified
        if (request.getPurpose() == OtpPurpose.OWNER_REGISTRATION) {
            Owner owner = ownerRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner account not found. Please register first."));

            if (owner.getIsVerified()) {
                throw new IllegalStateException("Account already verified. Please login.");
            }
        } else if (request.getPurpose() == OtpPurpose.USER_REGISTRATION) {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User account not found. Please register first."));

            if (user.getIsVerified()) {
                throw new IllegalStateException("Account already verified. Please login.");
            }
        }

        // Generate and send new OTP
        otpService.generateAndSendOtp(request.getEmail(), request.getPurpose());

        log.info("OTP resent successfully to: {}", request.getEmail());
    }

    /**
     * FORGOT PASSWORD - Step 1: Send OTP
     */
    @Transactional
    public void initiateForgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password initiated for email: {}", request.getEmail());

        // Check if email exists (but don't reveal if it doesn't - security best practice)
        boolean emailExists = ownerRepository.existsByEmail(request.getEmail())
                || userRepository.existsByEmail(request.getEmail());

        if (emailExists) {
            // Generate and send OTP
            otpService.generateAndSendOtp(request.getEmail(), OtpPurpose.RESET_PASSWORD);
        }

        // Always return success (prevent email enumeration)
        log.info("Password reset OTP process completed for: {}", request.getEmail());
    }

    /**
     * RESET PASSWORD - Step 2: Verify OTP and Update Password
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Resetting password for email: {}", request.getEmail());

        // Verify OTP
        otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpPurpose.RESET_PASSWORD);

        // Try to find as Owner
        Owner owner = ownerRepository.findByEmail(request.getEmail()).orElse(null);
        if (owner != null) {
            owner.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            ownerRepository.save(owner);

            // Cleanup OTP
            otpService.invalidateOtps(request.getEmail(), OtpPurpose.RESET_PASSWORD);

            log.info("Password reset successful for owner: {}", request.getEmail());
            return;
        }

        // Try to find as User
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user != null) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            // Cleanup OTP
            otpService.invalidateOtps(request.getEmail(), OtpPurpose.RESET_PASSWORD);

            log.info("Password reset successful for user: {}", request.getEmail());
            return;
        }

        throw new ResourceNotFoundException("Account not found with email: " + request.getEmail());
    }
}
