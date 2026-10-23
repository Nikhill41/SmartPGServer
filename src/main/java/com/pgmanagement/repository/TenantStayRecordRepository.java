package com.pgmanagement.repository;

import com.pgmanagement.entity.TenantStayRecord;
import com.pgmanagement.enums.StayStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantStayRecordRepository extends JpaRepository<TenantStayRecord, Long> {

    /**
     * Find active stay for tenant
     */
    Optional<TenantStayRecord> findByTenantIdAndStayStatus(Long tenantId, StayStatus status);

    /**
     * Find all stays for tenant
     */
    List<TenantStayRecord> findByTenantIdOrderByMoveInDateDesc(Long tenantId);

    /**
     * Find all stays in a room
     */
    List<TenantStayRecord> findByRoomIdOrderByMoveInDateDesc(Long roomId);

    /**
     * Find active stays in a room
     */
    List<TenantStayRecord> findByRoomIdAndStayStatus(Long roomId, StayStatus status);

    /**
     * Find all active stays in PG
     * ✅ FIXED: Changed r.pgId to r.pg.id
     */
    @Query("SELECT tsr FROM TenantStayRecord tsr " +
            "JOIN tsr.room r WHERE r.pg.id = :pgId " +
            "AND tsr.stayStatus = 'ACTIVE'")
    List<TenantStayRecord> findActiveStaysByPgId(@Param("pgId") Long pgId);

    /**
     * Check if tenant has active stay
     */
    @Query("SELECT CASE WHEN COUNT(tsr) > 0 THEN true ELSE false END " +
            "FROM TenantStayRecord tsr WHERE tsr.tenantId = :tenantId " +
            "AND tsr.stayStatus = 'ACTIVE'")
    boolean hasActiveStay(@Param("tenantId") Long tenantId);

    /**
     * Find stays overlapping with date range (for electricity calculation)
     */
    @Query("SELECT tsr FROM TenantStayRecord tsr WHERE tsr.roomId = :roomId " +
            "AND tsr.moveInDate <= :endDate " +
            "AND (tsr.moveOutDate IS NULL OR tsr.moveOutDate >= :startDate)")
    List<TenantStayRecord> findByRoomIdAndPeriodOverlap(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Count active tenants in room during period
     */
    @Query("SELECT COUNT(tsr) FROM TenantStayRecord tsr " +
            "WHERE tsr.roomId = :roomId " +
            "AND tsr.moveInDate <= :date " +
            "AND (tsr.moveOutDate IS NULL OR tsr.moveOutDate >= :date)")
    Long countActiveTenantsByRoomIdAndDate(
            @Param("roomId") Long roomId,
            @Param("date") LocalDate date
    );

    /**
     * Find stays by booking
     */
    Optional<TenantStayRecord> findByBookingId(Long bookingId);

    /**
     * Find pending settlements for PG
     * ✅ FIXED: Changed r.pgId to r.pg.id
     */
    @Query("SELECT tsr FROM TenantStayRecord tsr " +
            "JOIN tsr.room r WHERE r.pg.id = :pgId " +
            "AND tsr.stayStatus = 'PENDING_SETTLEMENT'")
    List<TenantStayRecord> findPendingSettlementsByPgId(@Param("pgId") Long pgId);

    /**
     * Find tenants with checkout initiated
     */
    @Query("SELECT tsr FROM TenantStayRecord tsr WHERE tsr.checkoutInitiated = true " +
            "AND tsr.stayStatus = 'ACTIVE'")
    List<TenantStayRecord> findCheckoutInitiated();
}
