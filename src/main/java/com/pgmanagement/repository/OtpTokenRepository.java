package com.pgmanagement.repository;

import com.pgmanagement.entity.OtpToken;
import com.pgmanagement.enums.OtpPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for OtpToken entity operations
 */
@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /**
     * Find latest OTP token for email and purpose
     */
    @Query("SELECT ot FROM OtpToken ot WHERE ot.email = :email AND ot.purpose = :purpose " +
            "AND ot.verified = false ORDER BY ot.createdAt DESC")
    Optional<OtpToken> findLatestOtpByEmailAndPurpose(@Param("email") String email,
                                                      @Param("purpose") OtpPurpose purpose);

    /**
     * Find valid (non-expired, unverified) OTP token
     */
    @Query("SELECT ot FROM OtpToken ot WHERE ot.email = :email AND ot.purpose = :purpose " +
            "AND ot.verified = false AND ot.expiresAt > :currentTime AND ot.attemptsLeft > 0 " +
            "ORDER BY ot.createdAt DESC")
    Optional<OtpToken> findValidOtp(@Param("email") String email,
                                    @Param("purpose") OtpPurpose purpose,
                                    @Param("currentTime") LocalDateTime currentTime);

    /**
     * Check if verified OTP exists for email and purpose
     */
    @Query("SELECT CASE WHEN COUNT(ot) > 0 THEN true ELSE false END FROM OtpToken ot " +
            "WHERE ot.email = :email AND ot.purpose = :purpose AND ot.verified = true")
    boolean hasVerifiedOtp(@Param("email") String email, @Param("purpose") OtpPurpose purpose);

    /**
     * Delete all OTPs for a specific email and purpose (cleanup)
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.email = :email AND ot.purpose = :purpose")
    void deleteByEmailAndPurpose(@Param("email") String email, @Param("purpose") OtpPurpose purpose);

    /**
     * Delete expired OTP tokens (cleanup job)
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.expiresAt < :currentTime")
    void deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Delete verified OTP tokens older than specified time
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM OtpToken ot WHERE ot.verified = true AND ot.createdAt < :cutoffTime")
    void deleteOldVerifiedTokens(@Param("cutoffTime") LocalDateTime cutoffTime);
}
