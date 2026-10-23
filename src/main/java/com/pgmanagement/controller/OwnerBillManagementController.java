package com.pgmanagement.controller;




import com.pgmanagement.dto.request.BulkBillGenerationRequest;
import com.pgmanagement.dto.request.EditBillRequest;
import com.pgmanagement.dto.request.GenerateBillRequest;
import com.pgmanagement.dto.response.BillDetailsResponse;
import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.dto.response.BulkBillGenerationResponse;
import com.pgmanagement.dto.response.MeterPreviewResponse;
import com.pgmanagement.entity.Bill;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.repository.BillRepository;
import com.pgmanagement.service.BillEditService;
import com.pgmanagement.service.BillGenerationService;
import com.pgmanagement.service.ElectricityCalculationService;
import com.pgmanagement.service.OverdueBillService;
import com.pgmanagement.security.JwtTokenProvider;
import com.pgmanagement.util.EntityMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/owner/billing/bills")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('OWNER')")
public class OwnerBillManagementController {
    private final EntityMapper entityMapper; // already exists in your project

    private final BillGenerationService billGenerationService;
    private final BillEditService billEditService;
    private final OverdueBillService overdueBillService;
    private final BillRepository billRepository;
    private final JwtTokenProvider jwtUtil;
    private final ElectricityCalculationService electricityCalculationService;


    /**
     * Generate manual bill for a tenant
     * POST /api/owner/billing/bills/generate/pg/{pgId}
     */
    @PostMapping("/generate/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> generateBill(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @Valid @RequestBody GenerateBillRequest request) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} generating manual bill for tenant {} in PG {}",
                ownerId, request.getTenantId(), pgId);

        BillResponse response = billGenerationService.generateBill(ownerId, pgId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Bill generated successfully");
        result.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Generate bulk bills for all active tenants
     * POST /api/owner/billing/bills/generate/bulk/pg/{pgId}
     */
    @PostMapping("/generate/bulk/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> generateBulkBills(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @Valid @RequestBody BulkBillGenerationRequest request) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} generating bulk bills for PG {}", ownerId, pgId);

        BulkBillGenerationResponse response = billGenerationService.generateBulkBills(
                ownerId, pgId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", String.format("Generated %d bills successfully, %d failures",
                response.getSuccessCount(), response.getFailureCount()));
        result.put("data", response);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Get all bills for a PG (paginated)
     * GET /api/owner/billing/bills/pg/{pgId}
     */
    @GetMapping("/pg/{pgId}")
    public ResponseEntity<Map<String, Object>> getBillsForPG(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BillStatus status) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching bills for PG {} (page {}, size {})", ownerId, pgId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("generatedDate").descending());
        Page<Bill> billsPage;

        if (status != null) {
            billsPage = billRepository.findByPgIdAndStatus(pgId, status, pageable);
        } else {
            billsPage = billRepository.findByPgId(pgId, pageable);
        }

        // ✅ Map raw entities to DTOs before returning
        List<BillResponse> billResponses = billsPage.getContent()
                .stream()
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", billResponses);
        result.put("currentPage", billsPage.getNumber());
        result.put("totalItems", billsPage.getTotalElements());
        result.put("totalPages", billsPage.getTotalPages());

        return ResponseEntity.ok(result);
    }

    /**
     * Get bill details with version history
     * GET /api/owner/billing/bills/{billId}
     */
    @GetMapping("/{billId}")
    public ResponseEntity<Map<String, Object>> getBillDetails(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching bill details for bill {}", ownerId, billId);

        BillDetailsResponse response = billEditService.getBillDetails(ownerId, billId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Edit bill (creates new version)
     * PUT /api/owner/billing/bills/{billId}
     */
    @PutMapping("/{billId}")
    public ResponseEntity<Map<String, Object>> editBill(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId,
            @Valid @RequestBody EditBillRequest request) throws IllegalStateException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} editing bill {}", ownerId, billId);

        BillDetailsResponse response = billEditService.editBill(ownerId, billId, request);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Bill updated successfully");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Waive late fee
     * POST /api/owner/billing/bills/{billId}/waive-late-fee
     */
    @PostMapping("/{billId}/waive-late-fee")
    public ResponseEntity<Map<String, Object>> waiveLateFee(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId,
            @RequestParam String reason) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} waiving late fee for bill {}", ownerId, billId);

        // ✅ map to DTO — no raw entity
        Bill bill = overdueBillService.waiveLateFee(ownerId, billId, reason);
        BillResponse response = entityMapper.toBillResponse(bill);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Late fee waived successfully");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Get overdue bills for PG
     * GET /api/owner/billing/bills/pg/{pgId}/overdue
     */
    @GetMapping("/pg/{pgId}/overdue")
    public ResponseEntity<Map<String, Object>> getOverdueBills(
            @RequestHeader("Authorization") String token,
            @PathVariable Long pgId) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching overdue bills for PG {}", ownerId, pgId);



        java.util.List<Bill> overdueBills = billRepository.findOverdueBillsByPgId(pgId);

        List<BillResponse> response = overdueBills.stream()
                .map(entityMapper::toBillResponse)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * Cancel bill
     * POST /api/owner/billing/bills/{billId}/cancel
     */
    @PostMapping("/{billId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBill(
            @RequestHeader("Authorization") String token,
            @PathVariable Long billId,
            @RequestParam @jakarta.validation.constraints.NotBlank String reason) throws IllegalStateException {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} cancelling bill {}", ownerId, billId);

        BillDetailsResponse response = billEditService.cancelBill(ownerId, billId, reason);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Bill cancelled successfully");
        result.put("data", response);

        return ResponseEntity.ok(result);
    }

    /**
     * ✅ NEW: Get meter preview for billing period
     * GET /api/owner/billing/bills/meter-preview
     */
    @GetMapping("/meter-preview")
    public ResponseEntity<Map<String, Object>> getMeterPreview(
            @RequestHeader("Authorization") String token,
            @RequestParam Long roomId,
            @RequestParam String start,
            @RequestParam String end) {

        Long ownerId = jwtUtil.extractUserId(token.substring(7));
        log.info("Owner {} fetching meter preview for room {} ({} to {})", ownerId, roomId, start, end);

        LocalDate periodStart = LocalDate.parse(start);
        LocalDate periodEnd = LocalDate.parse(end);

        MeterPreviewResponse preview = electricityCalculationService
                .getMeterPreview(roomId, periodStart, periodEnd);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", preview);

        return ResponseEntity.ok(result);
    }


}

