package com.pgmanagement.service;



import com.pgmanagement.constants.BillingErrorMessages;
import com.pgmanagement.entity.Bill;
import com.pgmanagement.entity.PGBillingConfig;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.repository.BillRepository;
import com.pgmanagement.repository.PGBillingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OverdueBillService {

    private final BillRepository billRepository;
    private final PGBillingConfigRepository billingConfigRepository;
    private final EmailService emailService;

    /**
     * Scheduled job: mark overdue bills and apply late fees.
     * Runs daily at 03:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void processOverdueBills() {
        LocalDate today = LocalDate.now();
        log.info("Overdue bill processor triggered for {}", today);

        // 1) Mark PENDING bills with dueDate < today as OVERDUE
        List<Bill> overdueCandidates = billRepository.findOverdueBills(today);
        for (Bill bill : overdueCandidates) {
            bill.setStatus(BillStatus.OVERDUE);
            bill.setUpdatedAt(java.time.LocalDateTime.now());
            billRepository.save(bill);

            log.info("Bill {} marked as OVERDUE", bill.getBillNumber());
            emailService.sendOverdueReminder(bill);
        }

        // 2) Apply late fees where configured
        applyLateFees(today);
        log.info("Overdue bill processor completed");
    }

    /**
     * Apply late fees according to each PG's config.
     */
    private void applyLateFees(LocalDate today) {
        // Find bills that are overdue and have no late fee yet.
        // Cutoff date will be used with each PG's late_fee_days.
        List<Bill> overdueBills = billRepository.findByStatus(BillStatus.OVERDUE);

        for (Bill bill : overdueBills) {
            try {
                PGBillingConfig config = billingConfigRepository.findByPgId(bill.getPgId())
                        .orElse(null);
                if (config == null || !Boolean.TRUE.equals(config.getLateFeeEnabled())) {
                    continue;
                }

                int graceDays = config.getLateFeeDays() != null ? config.getLateFeeDays() : 0;
                LocalDate lateFeeStartDate = bill.getDueDate().plusDays(graceDays);

                // Only apply late fee once when today >= lateFeeStartDate and no late fee applied yet
                if (bill.getLateFee() != null && bill.getLateFee().compareTo(BigDecimal.ZERO) > 0) {
                    continue;
                }
                if (today.isBefore(lateFeeStartDate)) {
                    continue;
                }

                BigDecimal lateFeeAmount = config.getLateFeeAmount() != null
                        ? config.getLateFeeAmount()
                        : BigDecimal.ZERO;
                if (lateFeeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                // Update bill with late fee and increase total amount
                bill.setLateFee(lateFeeAmount);
                bill.setTotalAmount(bill.getTotalAmount().add(lateFeeAmount));
                bill.setUpdatedAt(java.time.LocalDateTime.now());
                billRepository.save(bill);

                log.info("Late fee {} applied to bill {}", lateFeeAmount, bill.getBillNumber());
                emailService.sendLateFeeApplied(bill);

            } catch (Exception e) {
                log.error("Failed to apply late fee for bill {}: {}",
                        bill.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Allow owner to waive late fee for a specific bill.
     */
    @Transactional
    public Bill waiveLateFee(Long ownerId, Long billId, String reason) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new com.pgmanagement.exception.ResourceNotFoundException(
                        String.format(BillingErrorMessages.BILL_NOT_FOUND, billId)));

        if (!bill.getOwnerId().equals(ownerId)) {
            throw new com.pgmanagement.exception.ForbiddenException(
                    BillingErrorMessages.NOT_BILL_OWNER);
        }

        if (bill.getLateFee() == null || bill.getLateFee().compareTo(BigDecimal.ZERO) <= 0) {
            return bill;
        }

        BigDecimal previousLateFee = bill.getLateFee();
        bill.setTotalAmount(bill.getTotalAmount().subtract(previousLateFee));
        bill.setLateFee(BigDecimal.ZERO);
        bill.setUpdatedAt(java.time.LocalDateTime.now());
        billRepository.save(bill);

        log.info("Late fee waived for bill {} (amount {}, reason '{}')",
                bill.getBillNumber(), previousLateFee, reason);

        emailService.sendLateFeeWaived(bill, reason);
        return bill;
    }
}
