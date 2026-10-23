package com.pgmanagement.repository;

import com.pgmanagement.entity.Meter;
import com.pgmanagement.enums.MeterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Meter entity operations
 */
@Repository
public interface MeterRepository extends JpaRepository<Meter, Long> {

    /**
     * Find meter by meter ID
     */
    Optional<Meter> findByMeterId(String meterId);

    /**
     * Find meter by AWS Thing Name
     */
    Optional<Meter> findByAwsThingName(String awsThingName);

    /**
     * Find meter by room ID
     */
    Optional<Meter> findByRoomId(Long roomId);

    /**
     * Find all meters owned by a specific owner
     */
    List<Meter> findAllByOwnerId(Long ownerId);

    /**
     * Find all meters by owner and status
     */
    List<Meter> findAllByOwnerIdAndStatus(Long ownerId, MeterStatus status);

    /**
     * Check if meter ID already exists
     */
    boolean existsByMeterId(String meterId);

    /**
     * Check if room already has a meter
     */
    boolean existsByRoomId(Long roomId);

    /**
     * Verify meter belongs to owner
     */
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM Meter m " +
            "WHERE m.meterId = :meterId AND m.ownerId = :ownerId")
    boolean isMeterOwnedBy(@Param("meterId") String meterId, @Param("ownerId") Long ownerId);

    /**
     * Count active meters for owner
     */
    @Query("SELECT COUNT(m) FROM Meter m WHERE m.ownerId = :ownerId AND m.status = 'ACTIVE'")
    Long countActiveMetersForOwner(@Param("ownerId") Long ownerId);

    /**
     * Find all meters in a specific PG
     */
    @Query("SELECT m FROM Meter m WHERE m.room.pg.id = :pgId")
    List<Meter> findAllMetersInPG(@Param("pgId") Long pgId);
}
