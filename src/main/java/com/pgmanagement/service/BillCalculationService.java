package com.pgmanagement.service;



import com.pgmanagement.entity.PGBillingConfig;
import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.StandardCharge;
import com.pgmanagement.enums.ProRatedRentMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillCalculationService {

    /**
     * Calculate pro-rated or full rent
     */
    public BigDecimal calculateRent(Room room, LocalDate periodStart, LocalDate periodEnd,
                                    PGBillingConfig config, boolean isProRated) {
        BigDecimal monthlyRent = room.getRent();

        if (!isProRated) {
            log.debug("Calculating full month rent: {}", monthlyRent);
            return monthlyRent;
        }

        ProRatedRentMode mode = config.getProRatedRentMode();
        log.debug("Calculating pro-rated rent with mode: {}", mode);

        return switch (mode) {
            case FULL_MONTH -> {
                log.debug("FULL_MONTH mode: returning full rent");
                yield monthlyRent;
            }
            case HALF_MONTH -> {
                BigDecimal halfRent = monthlyRent.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                log.debug("HALF_MONTH mode: {}", halfRent);
                yield halfRent;
            }
            case DAILY -> {
                int daysInMonth = periodEnd.lengthOfMonth();
                long daysStayed = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                BigDecimal perDayRent = monthlyRent.divide(
                        BigDecimal.valueOf(daysInMonth), 4, RoundingMode.HALF_UP);
                BigDecimal proRatedRent = perDayRent.multiply(BigDecimal.valueOf(daysStayed))
                        .setScale(2, RoundingMode.HALF_UP);
                log.debug("DAILY mode: {} days at {} per day = {}", daysStayed, perDayRent, proRatedRent);
                yield proRatedRent;
            }
        };
    }

    /**
     * Calculate total other charges from standard charges
     */
    public BigDecimal calculateOtherCharges(List<StandardCharge> charges,
                                            LocalDate periodStart, LocalDate periodEnd,
                                            PGBillingConfig config, boolean isProRated) {
        BigDecimal total = BigDecimal.ZERO;

        for (StandardCharge charge : charges) {
            BigDecimal amount = charge.getAmount();

            // Pro-rate if configured and period is partial
            if (isProRated && config.getProRateStandardCharges()) {
                int daysInMonth = periodEnd.lengthOfMonth();
                long daysStayed = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                amount = amount.multiply(BigDecimal.valueOf(daysStayed))
                        .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
                log.debug("Pro-rated charge '{}': {} for {} days", charge.getChargeName(), amount, daysStayed);
            }

            total = total.add(amount);
        }

        log.debug("Total other charges: {}", total);
        return total;
    }

    /**
     * Calculate GST amount
     */
    public BigDecimal calculateGstAmount(BigDecimal subtotal, PGBillingConfig config) {
        if (!config.getGstEnabled() || config.getGstPercentage().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal gstAmount = subtotal
                .multiply(config.getGstPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        log.debug("GST calculated: {} on subtotal {} at {}%", gstAmount, subtotal, config.getGstPercentage());
        return gstAmount;
    }

    /**
     * Calculate bill totals
     */
    public BillTotals calculateBillTotals(BigDecimal roomRent,
                                          BigDecimal electricityCharge,
                                          BigDecimal otherCharges,
                                          BigDecimal discount,
                                          BigDecimal lateFee,
                                          BigDecimal additionalDeductions,
                                          PGBillingConfig config) {
        // Calculate subtotal
        BigDecimal subtotal = roomRent
                .add(electricityCharge)
                .add(otherCharges)
                .subtract(discount)
                .add(lateFee)
                .subtract(additionalDeductions);

        // Calculate GST
        BigDecimal gstAmount = calculateGstAmount(subtotal, config);

        // Calculate total
        BigDecimal totalAmount = subtotal.add(gstAmount);

        log.debug("Bill totals calculated - Subtotal: {}, GST: {}, Total: {}",
                subtotal, gstAmount, totalAmount);

        return new BillTotals(subtotal, config.getGstPercentage(), gstAmount, totalAmount);
    }

    /**
     * Inner class to hold calculation results
     */
    public record BillTotals(
            BigDecimal subtotal,
            BigDecimal gstPercentage,
            BigDecimal gstAmount,
            BigDecimal totalAmount
    ) {}
}

