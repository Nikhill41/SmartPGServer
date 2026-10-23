package com.pgmanagement.controller;

import com.pgmanagement.dto.response.BillDetailsResponse;
import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.entity.Bill;
import com.pgmanagement.entity.TenantStayRecord;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.StayStatus;
import com.pgmanagement.repository.BillRepository;
import com.pgmanagement.repository.TenantStayRecordRepository;
import com.pgmanagement.security.JwtTokenProvider;
import com.pgmanagement.service.BillEditService;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenant/billing/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class TenantBillingDashboardController {
    private final BillEditService billEditService; // ✅ ADD THIS

    private final BillRepository billRepository;
    private final TenantStayRecordRepository tenantStayRecordRepository;
    private final JwtTokenProvider jwtUtil;
    private final EntityMapper entityMapper; // ✅ ADDED

    /**
     * Get tenant billing dashboard
     * GET /api/tenant/billing/dashboard
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching billing dashboard", tenantId);

        TenantStayRecord activeStay = tenantStayRecordRepository
                .findByTenantIdAndStayStatus(tenantId, StayStatus.ACTIVE)
                .orElse(null);

        List<Bill> allBills = billRepository.findByTenantIdOrderByGeneratedDateDesc(tenantId);

        // ✅ All calculations done on raw entities BEFORE any serialization
        long pendingCount = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.OVERDUE)
                .count();

        long paidCount = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PAID)
                .count();

        long overdueCount = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.OVERDUE)
                .count();

        BigDecimal totalPending = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.OVERDUE)
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PAID)
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Bill nextDueBill = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.OVERDUE)
                .min((b1, b2) -> b1.getDueDate().compareTo(b2.getDueDate()))
                .orElse(null);

        Integer daysStayed = null;
        if (activeStay != null) {
            daysStayed = (int) ChronoUnit.DAYS.between(activeStay.getMoveInDate(), LocalDate.now());
        }

        Map<String, Object> dashboard = new HashMap<>();

        // Stay info — only primitives, no entities
        if (activeStay != null) {
            Map<String, Object> stayInfo = new HashMap<>();
            stayInfo.put("moveInDate", activeStay.getMoveInDate());
            stayInfo.put("daysStayed", daysStayed);
            stayInfo.put("checkoutInitiated", activeStay.getCheckoutInitiated());
            stayInfo.put("intendedMoveOutDate", activeStay.getIntendedMoveOutDate());
            dashboard.put("stayInfo", stayInfo);
        } else {
            dashboard.put("stayInfo", null);
        }

        // Bill statistics — only primitives/BigDecimal, safe
        Map<String, Object> billStats = new HashMap<>();
        billStats.put("totalBills", allBills.size());
        billStats.put("pendingCount", pendingCount);
        billStats.put("paidCount", paidCount);
        billStats.put("overdueCount", overdueCount);
        billStats.put("totalPendingAmount", totalPending);
        billStats.put("totalPaidAmount", totalPaid);
        dashboard.put("billStatistics", billStats);

        // Next due bill — ✅ only safe primitives extracted, no raw entity
        // ✅ FIXED — use BillDetailsResponse DTO directly
        if (nextDueBill != null) {
            BillDetailsResponse nextDueBillDto =
                    billEditService.getBillDetails(nextDueBill.getOwnerId(), nextDueBill.getId());
            dashboard.put("nextDueBill", nextDueBillDto);
        } else {
            dashboard.put("nextDueBill", null);
        }


        // ✅ Recent bills — mapped to DTOs, NOT raw entities
        List<BillResponse> recentBillDtos = allBills.stream()
                .limit(3)
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());
        dashboard.put("recentBills", recentBillDtos);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", dashboard);

        return ResponseEntity.ok(result);
    }

    /**
     * Get payment reminders
     * GET /api/tenant/billing/dashboard/reminders
     */
    @GetMapping("/reminders")
    public ResponseEntity<Map<String, Object>> getPaymentReminders(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching payment reminders", tenantId);

        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        List<Bill> allPending = billRepository.findByTenantIdAndStatus(tenantId, BillStatus.PENDING);
        allPending.addAll(billRepository.findByTenantIdAndStatus(tenantId, BillStatus.OVERDUE));

        List<Bill> overdueBills = allPending.stream()
                .filter(b -> b.getDueDate().isBefore(today))
                .toList();

        List<Bill> dueSoonBills = allPending.stream()
                .filter(b -> !b.getDueDate().isBefore(today) &&
                        b.getDueDate().isBefore(threeDaysFromNow))
                .toList();

        // ✅ Map to DTOs — NOT raw entities
        List<BillResponse> overdueDtos = overdueBills.stream()
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());

        List<BillResponse> dueSoonDtos = dueSoonBills.stream()
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());

        Map<String, Object> reminders = new HashMap<>();
        reminders.put("overdueBills", overdueDtos);    // ✅ DTOs
        reminders.put("dueSoonBills", dueSoonDtos);    // ✅ DTOs
        reminders.put("overdueCount", overdueDtos.size());
        reminders.put("dueSoonCount", dueSoonDtos.size());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", reminders);

        return ResponseEntity.ok(result);
    }
}
