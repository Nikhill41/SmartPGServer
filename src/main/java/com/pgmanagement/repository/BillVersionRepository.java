package com.pgmanagement.repository;



import com.pgmanagement.entity.BillVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillVersionRepository extends JpaRepository<BillVersion, Long> {

    /**
     * Find all versions for a bill (ordered by version number)
     */
    List<BillVersion> findByBillIdOrderByVersionNumberAsc(Long billId);

    /**
     * Find latest version for a bill
     */
    @Query("SELECT bv FROM BillVersion bv WHERE bv.billId = :billId " +
            "ORDER BY bv.versionNumber DESC LIMIT 1")
    BillVersion findLatestByBillId(@Param("billId") Long billId);

    /**
     * Count versions for a bill
     */
    Long countByBillId(Long billId);

    /**
     * Find versions by changed by owner
     */
    List<BillVersion> findByChangedBy(Long ownerId);
}

