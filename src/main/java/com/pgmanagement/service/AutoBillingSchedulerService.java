package com.pgmanagement.service;



import com.pgmanagement.dto.request.BulkBillGenerationRequest;
import com.pgmanagement.entity.PGBillingConfig;
import com.pgmanagement.enums.BillingMode;
import com.pgmanagement.repository.PGBillingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutoBillingSchedulerService {

    private final PGBillingConfigRepository billingConfigRepository;
    private final BillGenerationService billGenerationService;

    /**
     * Runs every day at 02:00 AM server time.
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void runDailyAutoBilling() {
        LocalDate today = LocalDate.now();
        int dayOfMonth = today.getDayOfMonth();
        YearMonth currentMonth = YearMonth.from(today);
        LocalDate periodStart = currentMonth.atDay(1);
        LocalDate periodEnd = currentMonth.atEndOfMonth();

        log.info("Auto-billing scheduler triggered for date: {}", today);

        // Find all PGs with AUTO billing and matching autoGenerationDay
        List<PGBillingConfig> configs =
                billingConfigRepository.findByBillingModeAndAutoGenerationDay(
                        BillingMode.AUTO, dayOfMonth);

        if (configs.isEmpty()) {
            log.info("No PGs configured for auto-billing on day {}", dayOfMonth);
            return;
        }

        for (PGBillingConfig config : configs) {
            Long pgId = config.getPgId();
            Long ownerId = config.getOwnerId();

            try {
                log.info("Running auto-billing for PG {} (owner {}) for period {} to {}",
                        pgId, ownerId, periodStart, periodEnd);

                BulkBillGenerationRequest request = new BulkBillGenerationRequest();
                request.setPgId(pgId);
                request.setBillingPeriodStart(periodStart);
                request.setBillingPeriodEnd(periodEnd);

                billGenerationService.generateBulkBills(ownerId, pgId, request);

            } catch (Exception e) {
                log.error("Auto-billing failed for PG {}: {}", pgId, e.getMessage(), e);
            }
        }

        log.info("Auto-billing scheduler completed for date: {}", today);
    }
}

