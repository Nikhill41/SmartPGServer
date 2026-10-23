package com.pgmanagement.repository;



import com.pgmanagement.entity.TenantElectricityShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantElectricityShareRepository extends JpaRepository<TenantElectricityShare, Long> {

    /**
     * Find electricity share for a bill
     */
    Optional<TenantElectricityShare> findByBillId(Long billId);

    /**
     * Find all shares for a tenant
     */
    List<TenantElectricityShare> findByTenantIdOrderByBillingPeriodStartDesc(Long tenantId);

    /**
     * Find shares for a room
     */
    List<TenantElectricityShare> findByRoomIdOrderByBillingPeriodStartDesc(Long roomId);

    /**
     * Find shares for tenant in date range
     */
    @Query("SELECT tes FROM TenantElectricityShare tes " +
            "WHERE tes.tenantId = :tenantId " +
            "AND tes.billingPeriodStart >= :startDate " +
            "AND tes.billingPeriodEnd <= :endDate")
    List<TenantElectricityShare> findByTenantIdAndDateRange(
            @Param("tenantId") Long tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total electricity consumption for tenant
     */
    @Query("SELECT COALESCE(SUM(tes.tenantUnitShare), 0) " +
            "FROM TenantElectricityShare tes " +
            "WHERE tes.tenantId = :tenantId")
    Double calculateTotalUnitsByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Calculate total electricity cost for tenant
     */
    @Query("SELECT COALESCE(SUM(tes.electricityCharge), 0) " +
            "FROM TenantElectricityShare tes " +
            "WHERE tes.tenantId = :tenantId")
    Double calculateTotalCostByTenantId(@Param("tenantId") Long tenantId);
}

