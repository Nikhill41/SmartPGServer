package com.pgmanagement.repository;



import com.pgmanagement.entity.PaymentProofHistory;
import com.pgmanagement.enums.PaymentProofStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentProofHistoryRepository extends JpaRepository<PaymentProofHistory, Long> {

    /**
     * Find all history for a bill (ordered by upload time)
     */
    List<PaymentProofHistory> findByBillIdOrderByUploadedAtDesc(Long billId);

    /**
     * Find latest upload for a bill
     */
    @Query("SELECT pph FROM PaymentProofHistory pph WHERE pph.billId = :billId " +
            "ORDER BY pph.uploadedAt DESC LIMIT 1")
    Optional<PaymentProofHistory> findLatestByBillId(@Param("billId") Long billId);

    /**
     * Count attempts for a bill
     */
    Long countByBillId(Long billId);

    /**
     * Find history by status
     */
    List<PaymentProofHistory> findByStatus(PaymentProofStatus status);

    /**
     * Find rejected proofs for a bill
     */
    @Query("SELECT pph FROM PaymentProofHistory pph WHERE pph.billId = :billId " +
            "AND pph.status = 'REJECTED' ORDER BY pph.uploadedAt DESC")
    List<PaymentProofHistory> findRejectedByBillId(@Param("billId") Long billId);

    /**
     * Count rejections for a bill
     */
    @Query("SELECT COUNT(pph) FROM PaymentProofHistory pph " +
            "WHERE pph.billId = :billId AND pph.status = 'REJECTED'")
    Long countRejectionsByBillId(@Param("billId") Long billId);

    /**
     * Find all proofs verified by owner
     */
    List<PaymentProofHistory> findByVerifiedBy(Long ownerId);
}

