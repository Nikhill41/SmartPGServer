package com.pgmanagement.repository;



import com.pgmanagement.entity.StandardCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StandardChargeRepository extends JpaRepository<StandardCharge, Long> {

    /**
     * Find all charges for a PG (including deleted)
     */
    List<StandardCharge> findByPgId(Long pgId);

    /**
     * Find all non-deleted charges for a PG
     */
    List<StandardCharge> findByPgIdAndIsDeletedFalse(Long pgId);

    /**
     * Find active charges for a PG (active = true, deleted = false)
     */
    @Query("SELECT sc FROM StandardCharge sc WHERE sc.pgId = :pgId " +
            "AND sc.isActive = true AND sc.isDeleted = false")
    List<StandardCharge> findActiveBypgId(@Param("pgId") Long pgId);

    /**
     * Find active and default charges for auto-billing
     */
    @Query("SELECT sc FROM StandardCharge sc WHERE sc.pgId = :pgId " +
            "AND sc.isActive = true AND sc.isDefault = true AND sc.isDeleted = false")
    List<StandardCharge> findActiveDefaultByPgId(@Param("pgId") Long pgId);

    /**
     * Find all charges by owner
     */
    List<StandardCharge> findByOwnerId(Long ownerId);

    /**
     * Find by PG and charge name (for duplicate check)
     */
    Optional<StandardCharge> findByPgIdAndChargeNameAndIsDeletedFalse(
            Long pgId,
            String chargeName
    );

    /**
     * Check if charge name exists for PG
     */
    @Query("SELECT CASE WHEN COUNT(sc) > 0 THEN true ELSE false END " +
            "FROM StandardCharge sc WHERE sc.pgId = :pgId " +
            "AND sc.chargeName = :chargeName AND sc.isDeleted = false " +
            "AND (:excludeId IS NULL OR sc.id != :excludeId)")
    boolean existsByPgIdAndChargeName(
            @Param("pgId") Long pgId,
            @Param("chargeName") String chargeName,
            @Param("excludeId") Long excludeId
    );

    /**
     * Count active charges for PG
     */
    @Query("SELECT COUNT(sc) FROM StandardCharge sc WHERE sc.pgId = :pgId " +
            "AND sc.isActive = true AND sc.isDeleted = false")
    Long countActiveByPgId(@Param("pgId") Long pgId);

    /**
     * Soft delete all charges for a PG
     */
    @Query("UPDATE StandardCharge sc SET sc.isDeleted = true " +
            "WHERE sc.pgId = :pgId")
    void softDeleteAllByPgId(@Param("pgId") Long pgId);
}

