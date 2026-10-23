package com.pgmanagement.repository;



import com.pgmanagement.entity.PGBillingConfig;
import com.pgmanagement.enums.BillingMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PGBillingConfigRepository extends JpaRepository<PGBillingConfig, Long> {

    /**
     * Find billing config by PG ID
     */
    Optional<PGBillingConfig> findByPgId(Long pgId);

    /**
     * Check if config exists for PG
     */
    boolean existsByPgId(Long pgId);

    /**
     * Find all configs for an owner
     */
    List<PGBillingConfig> findByOwnerId(Long ownerId);

    /**
     * Find all PGs with AUTO billing mode and specific auto generation day
     * Used by scheduled job for auto-billing
     */
    List<PGBillingConfig> findByBillingModeAndAutoGenerationDay(
            BillingMode billingMode,
            Integer autoGenerationDay
    );

    /**
     * Find all PGs with AUTO billing mode
     */
    List<PGBillingConfig> findByBillingMode(BillingMode billingMode);

    /**
     * Find configs with GST enabled
     */
    @Query("SELECT c FROM PGBillingConfig c WHERE c.gstEnabled = true AND c.ownerId = :ownerId")
    List<PGBillingConfig> findByOwnerIdAndGstEnabled(@Param("ownerId") Long ownerId);

    /**
     * Find configs by owner and billing mode
     */
    List<PGBillingConfig> findByOwnerIdAndBillingMode(Long ownerId, BillingMode billingMode);

    /**
     * Delete config by PG ID
     */
    void deleteByPgId(Long pgId);
}

