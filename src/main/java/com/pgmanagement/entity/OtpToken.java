package com.pgmanagement.entity;

import com.pgmanagement.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing OTP tokens for email verification and password reset
 * OTPs are BCrypt hashed for security and expire after 10 minutes
 */
@Entity
@Table(name = "otp_tokens", indexes = {
        @Index(name = "idx_otp_email_purpose", columnList = "email, purpose"),
        @Index(name = "idx_otp_expires", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "code_hash", nullable = false)
    private String codeHash; // BCrypt hashed OTP code

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "attempts_left", nullable = false)
    private Integer attemptsLeft = 5; // Max 5 attempts

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 10 minutes from creation

    @Column(nullable = false)
    private Boolean verified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if OTP has expired
     */
    @Transient
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * Check if attempts exhausted
     */
    @Transient
    public boolean hasAttemptsLeft() {
        return this.attemptsLeft > 0;
    }

    /**
     * Decrement attempt counter
     */
    public void decrementAttempts() {
        if (this.attemptsLeft > 0) {
            this.attemptsLeft--;
        }
    }
}
