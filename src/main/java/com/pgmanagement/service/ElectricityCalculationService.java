package com.pgmanagement.service;



import com.pgmanagement.dto.response.MeterPreviewResponse;
import com.pgmanagement.entity.Meter;
import com.pgmanagement.entity.PGBillingConfig;
import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.TenantStayRecord;
import com.pgmanagement.enums.ElectricitySplitMethod;
import com.pgmanagement.repository.MeterRepository;
import com.pgmanagement.repository.TenantStayRecordRepository;
import com.pgmanagement.service.aws.DynamoDbMeterReadingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ElectricityCalculationService {

    private final MeterRepository meterRepository;
    private final TenantStayRecordRepository tenantStayRecordRepository;
    private final DynamoDbMeterReadingService dynamoDbMeterReadingService;

    /**
     * Calculate electricity for a tenant
     */
    public ElectricityCalculation calculateElectricity(Room room, Long tenantId,
                                                       LocalDate periodStart, LocalDate periodEnd,
                                                       PGBillingConfig config) {
        // Check if electricity should be included
        if (!config.getIncludeElectricity()) {
            log.debug("Electricity billing disabled for PG ID: {}", config.getPgId());
            return new ElectricityCalculation(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }

        // Check if room has meter
        Meter meter = meterRepository.findByRoomId(room.getId()).orElse(null);
        if (meter == null) {
            log.warn("No meter found for room ID: {}", room.getId());
            return new ElectricityCalculation(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }

        try {
            // Get meter readings for period
            double totalRoomUnits = getTotalRoomUnits(meter.getMeterId(), periodStart, periodEnd);

            if (totalRoomUnits <= 0) {
                log.warn("No electricity consumption data for meter: {} in period {} to {}",
                        meter.getMeterId(), periodStart, periodEnd);
                return new ElectricityCalculation(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
            }

            // Get all stays in room during period
            List<TenantStayRecord> staysInPeriod = tenantStayRecordRepository
                    .findByRoomIdAndPeriodOverlap(room.getId(), periodStart, periodEnd);

            // Calculate tenant's share based on split method
            ElectricitySplitMethod splitMethod = config.getElectricitySplitMethod();

            double tenantUnits;
            int maxTenantCount;

            if (splitMethod == ElectricitySplitMethod.EQUAL) {
                // Simple equal split
                long activeTenantCount = staysInPeriod.size();
                tenantUnits = totalRoomUnits / activeTenantCount;
                maxTenantCount = (int) activeTenantCount;
                log.debug("Equal split: {} units / {} tenants = {} units per tenant",
                        totalRoomUnits, activeTenantCount, tenantUnits);
            } else {
                // Time-weighted split
                TimeWeightedSplit split = calculateTimeWeightedSplit(
                        tenantId, totalRoomUnits, periodStart, periodEnd, staysInPeriod);
                tenantUnits = split.tenantUnits();
                maxTenantCount = split.maxTenantCount();
            }

            // Calculate cost
            BigDecimal tenantUnitsBD = BigDecimal.valueOf(tenantUnits).setScale(2, RoundingMode.HALF_UP);
            BigDecimal electricityCost = tenantUnitsBD
                    .multiply(config.getElectricityRatePerUnit())
                    .setScale(2, RoundingMode.HALF_UP);

            log.info("Electricity calculated for tenant {}: {} units at {} per unit = {}",
                    tenantId, tenantUnitsBD, config.getElectricityRatePerUnit(), electricityCost);

            return new ElectricityCalculation(
                    BigDecimal.valueOf(totalRoomUnits).setScale(2, RoundingMode.HALF_UP),
                    tenantUnitsBD,
                    maxTenantCount,
                    electricityCost
            );

        } catch (Exception e) {
            log.error("Failed to calculate electricity for tenant {} in room {}", tenantId, room.getId(), e);
            return new ElectricityCalculation(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }
    }

    /**
     * Calculate time-weighted electricity split
     */
    private TimeWeightedSplit calculateTimeWeightedSplit(Long tenantId, double totalRoomUnits,
                                                         LocalDate periodStart, LocalDate periodEnd,
                                                         List<TenantStayRecord> staysInPeriod) {
        // Split period into sub-periods with constant occupancy
        List<SubPeriod> subPeriods = createSubPeriods(periodStart, periodEnd, staysInPeriod);

        double tenantTotalUnits = 0.0;
        int maxTenantCount = 0;

        for (SubPeriod subPeriod : subPeriods) {
            // Count active tenants in this sub-period
            int activeTenantsCount = countActiveTenants(staysInPeriod, subPeriod.start(), subPeriod.end());
            maxTenantCount = Math.max(maxTenantCount, activeTenantsCount);

            // Check if our tenant was active in this sub-period
            boolean tenantActive = isTenantActive(tenantId, staysInPeriod, subPeriod.start(), subPeriod.end());

            if (tenantActive && activeTenantsCount > 0) {
                // Calculate units for this sub-period
                long subPeriodDays = ChronoUnit.DAYS.between(subPeriod.start(), subPeriod.end()) + 1;
                long totalPeriodDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;

                double subPeriodUnits = totalRoomUnits * ((double) subPeriodDays / totalPeriodDays);
                double tenantShare = subPeriodUnits / activeTenantsCount;

                tenantTotalUnits += tenantShare;

                log.debug("Sub-period {} to {}: {} days, {} tenants, {} units, tenant share: {}",
                        subPeriod.start(), subPeriod.end(), subPeriodDays,
                        activeTenantsCount, subPeriodUnits, tenantShare);
            }
        }

        return new TimeWeightedSplit(tenantTotalUnits, maxTenantCount);
    }

    /**
     * Get total room units from DynamoDB
     */
    private double getTotalRoomUnits(String meterId, LocalDate periodStart, LocalDate periodEnd) {
        try {
            // Get readings at start and end of period
            double startReading = dynamoDbMeterReadingService.getMeterReadingAtDate(
                    meterId, periodStart.atStartOfDay());
            double endReading = dynamoDbMeterReadingService.getMeterReadingAtDate(
                    meterId, periodEnd.atTime(23, 59, 59));

            double totalUnits = endReading - startReading;
            log.debug("Meter {}: Start reading = {}, End reading = {}, Total units = {}",
                    meterId, startReading, endReading, totalUnits);

            return Math.max(0, totalUnits); // Ensure non-negative

        } catch (Exception e) {
            log.error("Failed to get meter readings for meter: {}", meterId, e);
            return 0.0;
        }
    }

    /**
     * Create sub-periods based on tenant join/leave events
     */
    private List<SubPeriod> createSubPeriods(LocalDate periodStart, LocalDate periodEnd,
                                             List<TenantStayRecord> stays) {
        List<SubPeriod> subPeriods = new ArrayList<>();
        List<LocalDate> changePoints = new ArrayList<>();

        changePoints.add(periodStart);
        changePoints.add(periodEnd.plusDays(1));

        // Add all move-in and move-out dates within period
        for (TenantStayRecord stay : stays) {
            if (stay.getMoveInDate().isAfter(periodStart) &&
                    stay.getMoveInDate().isBefore(periodEnd.plusDays(1))) {
                changePoints.add(stay.getMoveInDate());
            }
            if (stay.getMoveOutDate() != null &&
                    stay.getMoveOutDate().isAfter(periodStart) &&
                    stay.getMoveOutDate().isBefore(periodEnd.plusDays(1))) {
                changePoints.add(stay.getMoveOutDate().plusDays(1));
            }
        }

        // Sort and create sub-periods
        changePoints.sort(LocalDate::compareTo);
        for (int i = 0; i < changePoints.size() - 1; i++) {
            LocalDate start = changePoints.get(i);
            LocalDate end = changePoints.get(i + 1).minusDays(1);
            if (!start.isAfter(end)) {
                subPeriods.add(new SubPeriod(start, end));
            }
        }

        return subPeriods;
    }

    /**
     * Count active tenants in a period
     */
    private int countActiveTenants(List<TenantStayRecord> stays, LocalDate start, LocalDate end) {
        return (int) stays.stream()
                .filter(stay -> isStayActiveInPeriod(stay, start, end))
                .count();
    }

    /**
     * Check if tenant was active in period
     */
    private boolean isTenantActive(Long tenantId, List<TenantStayRecord> stays,
                                   LocalDate start, LocalDate end) {
        return stays.stream()
                .anyMatch(stay -> stay.getTenantId().equals(tenantId) &&
                        isStayActiveInPeriod(stay, start, end));
    }

    /**
     * Check if stay overlaps with period
     */
    private boolean isStayActiveInPeriod(TenantStayRecord stay, LocalDate start, LocalDate end) {
        LocalDate moveIn = stay.getMoveInDate();
        LocalDate moveOut = stay.getMoveOutDate();

        boolean startsBeforeOrDuringPeriod = !moveIn.isAfter(end);
        boolean endsAfterOrDuringPeriod = moveOut == null || !moveOut.isBefore(start);

        return startsBeforeOrDuringPeriod && endsAfterOrDuringPeriod;
    }

    // ========== INNER CLASSES ==========

    public record ElectricityCalculation(
            BigDecimal totalRoomUnits,
            BigDecimal tenantUnitShare,
            Integer totalRoomTenants,
            BigDecimal electricityCost
    ) {}

    private record SubPeriod(LocalDate start, LocalDate end) {}

    private record TimeWeightedSplit(double tenantUnits, int maxTenantCount) {}




    /**
     * ✅ NEW: Get meter preview for manual bill generation
     * Returns previousReading, currentReading, unitsConsumed from DynamoDB
     */
    public MeterPreviewResponse getMeterPreview(Long roomId, LocalDate periodStart, LocalDate periodEnd) {
        Meter meter = meterRepository.findByRoomId(roomId).orElse(null);

        if (meter == null) {
            return MeterPreviewResponse.builder()
                    .hasMeter(false)
                    .message("No meter paired with this room")
                    .build();
        }

        try {
            double previousReading = dynamoDbMeterReadingService.getMeterReadingAtDate(
                    meter.getMeterId(), periodStart.atStartOfDay());
            double currentReading = dynamoDbMeterReadingService.getMeterReadingAtDate(
                    meter.getMeterId(), periodEnd.atTime(23, 59, 59));
            double unitsConsumed = Math.max(0, currentReading - previousReading);

            return MeterPreviewResponse.builder()
                    .hasMeter(true)
                    .meterId(meter.getMeterId())
                    .previousReading(BigDecimal.valueOf(previousReading).setScale(2, RoundingMode.HALF_UP))
                    .currentReading(BigDecimal.valueOf(currentReading).setScale(2, RoundingMode.HALF_UP))
                    .unitsConsumed(BigDecimal.valueOf(unitsConsumed).setScale(2, RoundingMode.HALF_UP))
                    .message("Meter readings fetched successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to fetch meter preview for room {}", roomId, e);
            return MeterPreviewResponse.builder()
                    .hasMeter(true)
                    .meterId(meter.getMeterId())
                    .previousReading(BigDecimal.ZERO)
                    .currentReading(BigDecimal.ZERO)
                    .unitsConsumed(BigDecimal.ZERO)
                    .message("No meter data available for this period")
                    .build();
        }
    }

    /**
     * ✅ NEW: Calculate electricity with manual units override
     * Called from BillGenerationService when owner provides manual readings
     */
    public ElectricityCalculation calculateElectricityWithManualUnits(
            Room room, Long tenantId,
            LocalDate periodStart, LocalDate periodEnd,
            PGBillingConfig config,
            BigDecimal manualUnitsConsumed) {

        if (!config.getIncludeElectricity()) {
            return new ElectricityCalculation(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }

        try {
            double totalRoomUnits = manualUnitsConsumed.doubleValue();

            if (totalRoomUnits <= 0) {
                return new ElectricityCalculation(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
            }

            List<TenantStayRecord> staysInPeriod = tenantStayRecordRepository
                    .findByRoomIdAndPeriodOverlap(room.getId(), periodStart, periodEnd);

            ElectricitySplitMethod splitMethod = config.getElectricitySplitMethod();
            double tenantUnits;
            int maxTenantCount;

            if (splitMethod == ElectricitySplitMethod.EQUAL) {
                long activeTenantCount = Math.max(1, staysInPeriod.size());
                tenantUnits = totalRoomUnits / activeTenantCount;
                maxTenantCount = (int) activeTenantCount;
            } else {
                TimeWeightedSplit split = calculateTimeWeightedSplit(
                        tenantId, totalRoomUnits, periodStart, periodEnd, staysInPeriod);
                tenantUnits = split.tenantUnits();
                maxTenantCount = split.maxTenantCount();
            }

            BigDecimal tenantUnitsBD = BigDecimal.valueOf(tenantUnits).setScale(2, RoundingMode.HALF_UP);
            BigDecimal electricityCost = tenantUnitsBD
                    .multiply(config.getElectricityRatePerUnit())
                    .setScale(2, RoundingMode.HALF_UP);

            log.info("Manual electricity calc for tenant {}: {} units (manual override) = {}",
                    tenantId, tenantUnitsBD, electricityCost);

            return new ElectricityCalculation(
                    manualUnitsConsumed.setScale(2, RoundingMode.HALF_UP),
                    tenantUnitsBD,
                    maxTenantCount,
                    electricityCost
            );

        } catch (Exception e) {
            log.error("Failed manual electricity calc for tenant {} in room {}", tenantId, room.getId(), e);
            return new ElectricityCalculation(BigDecimal.ZERO, BigDecimal.ZERO, 0, BigDecimal.ZERO);
        }
    }

}
