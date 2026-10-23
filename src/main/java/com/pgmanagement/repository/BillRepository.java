package com.pgmanagement.repository;


import com.pgmanagement.entity.Bill;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.BillType;
import com.pgmanagement.enums.PaymentProofStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    List<Bill> findByStatus(BillStatus status);

    /**
     * Find bill by bill number
     */
    Optional<Bill> findByBillNumber(String billNumber);

    /**
     * Check if bill number exists
     */
    boolean existsByBillNumber(String billNumber);

    // ========== OWNER QUERIES ==========

    /**
     * Find all bills for a PG
     */
    Page<Bill> findByPgId(Long pgId, Pageable pageable);

    /**
     * Find bills by PG and status
     */
    Page<Bill> findByPgIdAndStatus(Long pgId, BillStatus status, Pageable pageable);

    /**
     * Find bills by PG and tenant
     */
    Page<Bill> findByPgIdAndTenantId(Long pgId, Long tenantId, Pageable pageable);

    /**
     * Find bills by owner
     */
    List<Bill> findByOwnerId(Long ownerId);

    /**
     * Find pending verification bills for PG
     */
    @Query("SELECT b FROM Bill b WHERE b.pgId = :pgId " +
            "AND b.paymentProofStatus = 'UPLOADED' AND b.status = 'PENDING'")
    List<Bill> findPendingVerificationsByPgId(@Param("pgId") Long pgId);

    /**
     * Find overdue bills for PG
     */
    @Query("SELECT b FROM Bill b WHERE b.pgId = :pgId " +
            "AND b.status = 'OVERDUE'")
    List<Bill> findOverdueBillsByPgId(@Param("pgId") Long pgId);

    // ========== TENANT QUERIES ==========

    /**
     * Find all bills for a tenant
     */
    List<Bill> findByTenantIdOrderByGeneratedDateDesc(Long tenantId);

    /**
     * Find bills by tenant and status
     */
    List<Bill> findByTenantIdAndStatus(Long tenantId, BillStatus status);

    /**
     * Find bills by tenant and bill type
     */
    List<Bill> findByTenantIdAndBillType(Long tenantId, BillType billType);

    /**
     * Find security deposit bill for tenant
     */
    @Query("SELECT b FROM Bill b WHERE b.tenantId = :tenantId " +
            "AND b.billType = 'SECURITY_DEPOSIT' " +
            "ORDER BY b.generatedDate DESC")
    List<Bill> findSecurityDepositBillsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Check if tenant has pending bills
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Bill b WHERE b.tenantId = :tenantId " +
            "AND b.status IN ('PENDING', 'OVERDUE')")
    boolean hasPendingBills(@Param("tenantId") Long tenantId);

    /**
     * Count pending bills for tenant
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.tenantId = :tenantId " +
            "AND b.status IN ('PENDING', 'OVERDUE')")
    Long countPendingBillsByTenantId(@Param("tenantId") Long tenantId);

    // ========== BILLING PERIOD QUERIES ==========

    /**
     * Check if bill exists for tenant in period
     */
    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END " +
            "FROM Bill b WHERE b.tenantId = :tenantId " +
            "AND b.billingPeriodStart = :start AND b.billingPeriodEnd = :end " +
            "AND b.status != 'CANCELLED'")
    boolean existsByTenantIdAndBillingPeriod(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /**
     * Find bills for period
     */
    @Query("SELECT b FROM Bill b WHERE b.tenantId = :tenantId " +
            "AND b.billingPeriodStart = :start AND b.billingPeriodEnd = :end " +
            "AND b.status != 'CANCELLED'")
    List<Bill> findByTenantIdAndBillingPeriod(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    /**
     * Cancel bills in date range for tenant
     */
    @Modifying
    @Query("UPDATE Bill b SET b.status = 'CANCELLED', " +
            "b.cancellationReason = :reason, b.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE b.tenantId = :tenantId " +
            "AND b.billingPeriodStart >= :start AND b.billingPeriodEnd <= :end " +
            "AND b.status = 'PENDING'")
    int cancelBillsForPeriod(
            @Param("tenantId") Long tenantId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("reason") String reason
    );

    // ========== OVERDUE & LATE FEE QUERIES ==========

    /**
     * Find all pending bills past due date
     */
    @Query("SELECT b FROM Bill b WHERE b.status = 'PENDING' " +
            "AND b.dueDate < :today")
    List<Bill> findOverdueBills(@Param("today") LocalDate today);

    /**
     * Find bills for late fee application
     */
    @Query("SELECT b FROM Bill b WHERE b.status = 'OVERDUE' " +
            "AND b.lateFee = 0 AND b.dueDate < :cutoffDate")
    List<Bill> findBillsForLateFeeApplication(@Param("cutoffDate") LocalDate cutoffDate);

    // ========== STATISTICS QUERIES ==========

    /**
     * Calculate total billed amount for PG in month
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b " +
            "WHERE b.pgId = :pgId " +
            "AND YEAR(b.generatedDate) = :year AND MONTH(b.generatedDate) = :month " +
            "AND b.status != 'CANCELLED'")
    Double calculateTotalBilledForMonth(
            @Param("pgId") Long pgId,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Calculate total collected amount for PG in month
     */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b " +
            "WHERE b.pgId = :pgId " +
            "AND YEAR(b.generatedDate) = :year AND MONTH(b.generatedDate) = :month " +
            "AND b.status = 'PAID'")
    Double calculateTotalCollectedForMonth(
            @Param("pgId") Long pgId,
            @Param("year") int year,
            @Param("month") int month
    );

    /**
     * Count bills by status for PG
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.pgId = :pgId AND b.status = :status")
    Long countByPgIdAndStatus(@Param("pgId") Long pgId, @Param("status") BillStatus status);

    /**
     * Get monthly statistics for PG
     */
    @Query("SELECT new map(" +
            "COALESCE(SUM(CASE WHEN b.status != 'CANCELLED' THEN b.totalAmount ELSE 0 END), 0) as totalBilled, " +
            "COALESCE(SUM(CASE WHEN b.status = 'PAID' THEN b.totalAmount ELSE 0 END), 0) as collected, " +
            "COALESCE(SUM(CASE WHEN b.status = 'PENDING' THEN b.totalAmount ELSE 0 END), 0) as pending, " +
            "COALESCE(SUM(CASE WHEN b.status = 'OVERDUE' THEN b.totalAmount ELSE 0 END), 0) as overdue, " +
            "COUNT(b) as totalBills" +
            ") FROM Bill b " +
            "WHERE b.pgId = :pgId " +
            "AND YEAR(b.generatedDate) = :year AND MONTH(b.generatedDate) = :month")
    Object getMonthlyStatistics(
            @Param("pgId") Long pgId,
            @Param("year") int year,
            @Param("month") int month
    );

    // ========== ROOM QUERIES ==========

    /**
     * Find bills for a room
     */
    List<Bill> findByRoomIdOrderByGeneratedDateDesc(Long roomId);

    /**
     * Find active bills for room
     */
    @Query("SELECT b FROM Bill b WHERE b.roomId = :roomId " +
            "AND b.status IN ('PENDING', 'OVERDUE')")
    List<Bill> findActiveBillsByRoomId(@Param("roomId") Long roomId);

    List<Bill> findByPgIdAndPaymentProofStatus(Long pgId, PaymentProofStatus status);

}

