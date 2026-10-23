package com.pgmanagement.controller;

import com.pgmanagement.dto.response.BillDetailsResponse;
import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.entity.Bill;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.BillType;
import com.pgmanagement.repository.BillRepository;
import com.pgmanagement.service.BillEditService;
import com.pgmanagement.security.JwtTokenProvider;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenant/billing/bills")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('USER')")
public class TenantBillController {

    private final BillRepository billRepository;
    private final BillEditService billEditService;
    private final JwtTokenProvider jwtUtil;
    private final EntityMapper entityMapper; // ✅ ADDED

    /**
     * Get all my bills
     * GET /api/tenant/billing/bills
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getMyBills(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(required = false) BillType billType) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching bills (status: {}, type: {})", tenantId, status, billType);

        List<Bill> bills;

        if (status != null) {
            bills = billRepository.findByTenantIdAndStatus(tenantId, status);
        } else if (billType != null) {
            bills = billRepository.findByTenantIdAndBillType(tenantId, billType);
        } else {
            bills = billRepository.findByTenantIdOrderByGeneratedDateDesc(tenantId);
        }

        // ✅ Map to DTOs
        List<BillResponse> billDtos = bills.stream()
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", billDtos);    // ✅ DTOs not entities
        result.put("count", billDtos.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Get pending bills
     * GET /api/tenant/billing/bills/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingBills(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching pending bills", tenantId);

        List<Bill> pendingBills = billRepository.findByTenantIdAndStatus(tenantId, BillStatus.PENDING);
        List<Bill> overdueBills = billRepository.findByTenantIdAndStatus(tenantId, BillStatus.OVERDUE);
        pendingBills.addAll(overdueBills);

        // ✅ Map to DTOs
        List<BillResponse> pendingDtos = pendingBills.stream()
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());

        java.math.BigDecimal totalDue = pendingBills.stream()
                .map(Bill::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", pendingDtos);    // ✅ DTOs not entities
        result.put("count", pendingDtos.size());
        result.put("totalAmountDue", totalDue);

        return ResponseEntity.ok(result);
    }

    /**
     * Get paid bills
     * GET /api/tenant/billing/bills/paid
     */
    @GetMapping("/paid")
    public ResponseEntity<Map<String, Object>> getPaidBills(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching paid bills", tenantId);

        List<Bill> paidBills = billRepository.findByTenantIdAndStatus(tenantId, BillStatus.PAID);

        // ✅ Map to DTOs
        List<BillResponse> paidDtos = paidBills.stream()
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", paidDtos);    // ✅ DTOs not entities
        result.put("count", paidDtos.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Get bill details
     * GET /api/tenant/billing/bills/{billId}
     */
    @GetMapping("/{billId}")
    public ResponseEntity<Map<String, Object>> getBillDetails(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching bill details for bill {}", tenantId, billId);

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new com.pgmanagement.exception.ResourceNotFoundException(
                        "Bill not found with ID: " + billId));

        if (!bill.getTenantId().equals(tenantId)) {
            throw new com.pgmanagement.exception.ForbiddenException(
                    "This bill does not belong to you");
        }

        // ✅ Already returns BillDetailsResponse — no change needed here
        BillDetailsResponse response = billEditService.getBillDetails(bill.getOwnerId(), billId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Get security deposit bill
     * GET /api/tenant/billing/bills/security-deposit
     */
    @GetMapping("/security-deposit")
    public ResponseEntity<Map<String, Object>> getSecurityDepositBill(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching security deposit bill", tenantId);

        List<Bill> securityDepositBills = billRepository.findSecurityDepositBillsByTenantId(tenantId);

        // ✅ Map to DTO
        BillResponse dto = securityDepositBills.isEmpty()
                ? null
                : entityMapper.toBillResponse(securityDepositBills.get(0));

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", dto);    // ✅ DTO not entity

        return ResponseEntity.ok(result);
    }

    /**
     * Check if I have pending bills
     * GET /api/tenant/billing/bills/has-pending
     */
    @GetMapping("/has-pending")
    public ResponseEntity<Map<String, Object>> hasPendingBills(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} checking for pending bills", tenantId);

        boolean hasPending = billRepository.hasPendingBills(tenantId);
        Long pendingCount = billRepository.countPendingBillsByTenantId(tenantId);

        // ✅ No entity returned — already safe
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("hasPendingBills", hasPending);
        result.put("pendingCount", pendingCount);

        return ResponseEntity.ok(result);
    }

    /**
     * Get bill summary statistics
     * GET /api/tenant/billing/bills/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBillSummary(
            @RequestHeader("Authorization") String token) {

        Long tenantId = jwtUtil.extractUserId(token.substring(7));
        log.info("Tenant {} fetching bill summary", tenantId);

        List<Bill> allBills = billRepository.findByTenantIdOrderByGeneratedDateDesc(tenantId);

        // ✅ Calculations done on entities BEFORE they go anywhere near Jackson
        long pendingCount = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.OVERDUE)
                .count();

        long paidCount = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PAID)
                .count();

        long overdueCount = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.OVERDUE)
                .count();

        java.math.BigDecimal totalPending = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PENDING || b.getStatus() == BillStatus.OVERDUE)
                .map(Bill::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal totalPaid = allBills.stream()
                .filter(b -> b.getStatus() == BillStatus.PAID)
                .map(Bill::getTotalAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // ✅ Only primitives/BigDecimal in summary map — no entities
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalBills", allBills.size());
        summary.put("pendingCount", pendingCount);
        summary.put("paidCount", paidCount);
        summary.put("overdueCount", overdueCount);
        summary.put("totalPendingAmount", totalPending);
        summary.put("totalPaidAmount", totalPaid);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", summary);

        return ResponseEntity.ok(result);
    }
}
