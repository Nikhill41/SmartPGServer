package com.pgmanagement.controller;

import com.pgmanagement.dto.request.*;
import com.pgmanagement.dto.response.ApiResponse;
import com.pgmanagement.dto.response.AuthResponse;
import com.pgmanagement.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * NEW FLOW: Register → Verify OTP → Auto-login
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    // ==================== OWNER REGISTRATION ====================

    /**
     * Owner Registration (Creates unverified account + sends OTP)
     */
    @PostMapping("/owner/register")
    public ResponseEntity<ApiResponse<Void>> registerOwner(
            @Valid @RequestBody OwnerRegistrationRequest request) {

        log.info("Owner registration requested for email: {}", request.getEmail());
        authService.registerOwner(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully! Please check your email for OTP to verify your account."));
    }

    // ==================== USER REGISTRATION ====================

    /**
     * User Registration (Creates unverified account + sends OTP)
     */
    @PostMapping("/user/register")
    public ResponseEntity<ApiResponse<Void>> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {

        log.info("User registration requested for email: {}", request.getEmail());
        authService.registerUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully! Please check your email for OTP to verify your account."));
    }

    // ==================== OTP VERIFICATION ====================

    /**
     * Verify OTP (Verifies account + returns JWT for auto-login)
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody OtpVerificationRequest request) {

        log.info("OTP verification requested for email: {}", request.getEmail());
        AuthResponse authResponse = authService.verifyRegistrationOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("Email verified successfully! You are now logged in.", authResponse)
        );
    }

    /**
     * Resend OTP
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request) {

        log.info("Resend OTP requested for email: {}", request.getEmail());
        authService.resendOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("OTP resent successfully. Please check your email.")
        );
    }

    // ==================== LOGIN ====================

    /**
     * Login (works for both Owner and User - ONLY verified accounts)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login attempt for email: {}", request.getEmail());
        AuthResponse authResponse = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authResponse)
        );
    }

    // ==================== PASSWORD RESET ====================

    /**
     * Step 1: Forgot password (send OTP)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("Forgot password requested for email: {}", request.getEmail());
        authService.initiateForgotPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("If the email exists, an OTP has been sent for password reset")
        );
    }

    /**
     * Step 2: Reset password (verify OTP + set new password)
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("Password reset requested for email: {}", request.getEmail());
        authService.resetPassword(request);

        return ResponseEntity.ok(
                ApiResponse.success("Password reset successful. You can now login with your new password.")
        );
    }
}
