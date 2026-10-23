package com.pgmanagement.repository;



import com.pgmanagement.entity.BillCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillChargeRepository extends JpaRepository<BillCharge, Long> {

    /**
     * Find all charges for a bill
     */
    List<BillCharge> findByBillId(Long billId);

    /**
     * Delete all charges for a bill
     */
    @Modifying
    @Query("DELETE FROM BillCharge bc WHERE bc.billId = :billId")
    void deleteByBillId(@Param("billId") Long billId);

    /**
     * Count charges for a bill
     */
    Long countByBillId(Long billId);

    /**
     * Find charges by name
     */
    List<BillCharge> findByChargeName(String chargeName);
}

