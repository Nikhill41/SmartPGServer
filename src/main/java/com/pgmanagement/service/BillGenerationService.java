package com.pgmanagement.service;



import com.pgmanagement.constants.BillingErrorMessages;

import com.pgmanagement.dto.request.BulkBillGenerationRequest;
import com.pgmanagement.dto.response.BillGenerationError;
import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.dto.request.GenerateBillRequest;
import com.pgmanagement.dto.response.BulkBillGenerationResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.BillType;
import com.pgmanagement.enums.GenerationType;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillGenerationService {

    private final BillRepository billRepository;
    private final PGBillingConfigRepository billingConfigRepository;
    private final StandardChargeRepository standardChargeRepository;
    private final TenantStayRecordRepository tenantStayRecordRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PGRepository pgRepository;
    private final BillChargeRepository billChargeRepository;
    private final TenantElectricityShareRepository tenantElectricityShareRepository;

    private final BillCalculationService billCalculationService;
    private final ElectricityCalculationService electricityCalculationService;
    private final BillNumberGeneratorService billNumberGeneratorService;
    private final UpiPaymentLinkService upiPaymentLinkService;

    /**
     * Generate manual bill
     */
    @Transactional
    public BillResponse generateBill(Long ownerId, Long pgId, GenerateBillRequest request) {
        log.info("Generating manual bill for tenant {} in PG {}", request.getTenantId(), pgId);

        // Verify ownership
        PG pg = verifyPGOwnership(ownerId, pgId);

        // Get billing config
        PGBillingConfig config = billingConfigRepository.findByPgId(pgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CONFIG_NOT_FOUND, pgId)));

        // Verify tenant and room
        User tenant = userRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (!room.getPg().getId().equals(pgId)) {
            throw new ForbiddenException("Room does not belong to this PG");
        }

        // Check if bill already exists for this period
        if (billRepository.existsByTenantIdAndBillingPeriod(
                request.getTenantId(), request.getBillingPeriodStart(), request.getBillingPeriodEnd())) {
            throw new IllegalStateException(
                    String.format(BillingErrorMessages.BILL_ALREADY_EXISTS_FOR_PERIOD,
                            request.getBillingPeriodStart(), request.getBillingPeriodEnd()));
        }

        // Get tenant stay record
        TenantStayRecord stay = tenantStayRecordRepository.findByTenantIdAndStayStatus(
                        request.getTenantId(), com.pgmanagement.enums.StayStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(BillingErrorMessages.NO_ACTIVE_STAY));

        // Determine if pro-rated
        boolean isProRated = isProRatedPeriod(stay.getMoveInDate(),
                request.getBillingPeriodStart(), request.getBillingPeriodEnd());

        // Calculate charges
        BigDecimal roomRent = billCalculationService.calculateRent(
                room, request.getBillingPeriodStart(), request.getBillingPeriodEnd(), config, isProRated);

        // Calculate electricity
        // ✅ NEW: Use manual readings if owner provided them, else auto-fetch from DynamoDB
        ElectricityCalculationService.ElectricityCalculation electricityCalc =
                request.hasManualReadings()
                        ? electricityCalculationService.calculateElectricityWithManualUnits(
                        room, request.getTenantId(),
                        request.getBillingPeriodStart(), request.getBillingPeriodEnd(),
                        config, request.getManualUnitsConsumed())
                        : electricityCalculationService.calculateElectricity(
                        room, request.getTenantId(),
                        request.getBillingPeriodStart(), request.getBillingPeriodEnd(), config);


        // Get standard charges
        List<StandardCharge> standardCharges = standardChargeRepository.findActiveDefaultByPgId(pgId);
        BigDecimal otherCharges = billCalculationService.calculateOtherCharges(
                standardCharges, request.getBillingPeriodStart(), request.getBillingPeriodEnd(),
                config, isProRated);

        // Calculate totals
        BillCalculationService.BillTotals totals = billCalculationService.calculateBillTotals(
                roomRent,
                electricityCalc.electricityCost(),
                otherCharges,
                request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO,
                BigDecimal.ZERO, // No late fee on generation
                BigDecimal.ZERO, // No additional deductions
                config
        );

        // Create bill
        Bill bill = new Bill();
        bill.setBillNumber(billNumberGeneratorService.generateBillNumber(
                pg, room, BillType.MONTHLY, LocalDate.now()));
        bill.setTenantId(request.getTenantId());
        bill.setRoomId(request.getRoomId());
        bill.setPgId(pgId);
        bill.setOwnerId(ownerId);
        bill.setBillingPeriodStart(request.getBillingPeriodStart());
        bill.setBillingPeriodEnd(request.getBillingPeriodEnd());
        bill.setRoomRent(roomRent);
        bill.setElectricityCharge(electricityCalc.electricityCost());
        bill.setOtherCharges(otherCharges);
        bill.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        bill.setLateFee(BigDecimal.ZERO);
        bill.setAdditionalDeductions(BigDecimal.ZERO);
        bill.setSubtotal(totals.subtotal());
        bill.setGstEnabled(config.getGstEnabled());
        bill.setGstPercentage(config.getGstPercentage());
        bill.setGstAmount(totals.gstAmount());
        bill.setSecurityDepositUsed(BigDecimal.ZERO);
        bill.setTotalAmount(totals.totalAmount());
        bill.setTotalRoomUnits(electricityCalc.totalRoomUnits());
        bill.setTenantUnitShare(electricityCalc.tenantUnitShare());
        bill.setTotalRoomTenants(electricityCalc.totalRoomTenants());
        bill.setStatus(BillStatus.PENDING);
        bill.setBillType(BillType.MONTHLY);
        bill.setGenerationType(GenerationType.MANUAL);
        bill.setDueDate(LocalDate.now().plusDays(config.getDueDaysOffset()));
        bill.setGeneratedDate(LocalDate.now());
        bill.setNotes(request.getNotes());
        bill.setCreatedAt(LocalDateTime.now());

        Bill savedBill = billRepository.save(bill);

        // Save bill charges
        saveBillCharges(savedBill.getId(), standardCharges, isProRated,
                request.getBillingPeriodStart(), request.getBillingPeriodEnd(), config);

        // Save electricity share
        if (electricityCalc.electricityCost().compareTo(BigDecimal.ZERO) > 0) {
            saveElectricityShare(savedBill, electricityCalc, request.getBillingPeriodStart(),
                    request.getBillingPeriodEnd());
        }

        log.info("Bill generated successfully: {}", savedBill.getBillNumber());

        return mapBillToResponse(savedBill, tenant, room, pg, config);
    }

    /**
     * Generate bulk bills for all active tenants in PG
     */
    @Transactional
    public BulkBillGenerationResponse generateBulkBills(Long ownerId, Long pgId,
                                                        BulkBillGenerationRequest request) {
        log.info("Generating bulk bills for PG {} by owner {}", pgId, ownerId);

        PG pg = verifyPGOwnership(ownerId, pgId);

        // Get all active stays in PG
        List<TenantStayRecord> activeStays = tenantStayRecordRepository.findActiveStaysByPgId(pgId);

        List<BillResponse> generatedBills = new ArrayList<>();
        List<BillGenerationError> errors = new ArrayList<>();

        for (TenantStayRecord stay : activeStays) {
            try {
                GenerateBillRequest billRequest = new GenerateBillRequest();
                billRequest.setTenantId(stay.getTenantId());
                billRequest.setRoomId(stay.getRoomId());
                billRequest.setBillingPeriodStart(request.getBillingPeriodStart());
                billRequest.setBillingPeriodEnd(request.getBillingPeriodEnd());

                BillResponse bill = generateBill(ownerId, pgId, billRequest);
                generatedBills.add(bill);

            } catch (Exception e) {
                log.error("Failed to generate bill for tenant {}", stay.getTenantId(), e);
                User tenant = userRepository.findById(stay.getTenantId()).orElse(null);
                errors.add(BillGenerationError.builder()
                        .tenantId(stay.getTenantId())
                        .tenantName(tenant != null ? tenant.getName() : "Unknown")
                        .error(e.getMessage())
                        .build());
            }
        }

        log.info("Bulk bill generation completed: {} succeeded, {} failed",
                generatedBills.size(), errors.size());

        return BulkBillGenerationResponse.builder()
                .totalTenants(activeStays.size())
                .successCount(generatedBills.size())
                .failureCount(errors.size())
                .generatedBills(generatedBills)
                .errors(errors)
                .build();
    }

    // ========== HELPER METHODS ==========

    private PG verifyPGOwnership(Long ownerId, Long pgId) {
        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new ResourceNotFoundException("PG not found with ID: " + pgId));

        if (!pg.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_PG_OWNER);
        }

        return pg;
    }

    private boolean isProRatedPeriod(LocalDate moveInDate, LocalDate periodStart, LocalDate periodEnd) {
        // Pro-rated if tenant moved in during this billing period
        return moveInDate.isAfter(periodStart) && moveInDate.isBefore(periodEnd.plusDays(1));
    }

    private void saveBillCharges(Long billId, List<StandardCharge> charges, boolean isProRated,
                                 LocalDate periodStart, LocalDate periodEnd, PGBillingConfig config) {
        for (StandardCharge charge : charges) {
            BillCharge billCharge = new BillCharge();
            billCharge.setBillId(billId);
            billCharge.setChargeName(charge.getChargeName());

            BigDecimal amount = charge.getAmount();
            boolean chargeIsProRated = false;

            if (isProRated && config.getProRateStandardCharges()) {
                int daysInMonth = periodEnd.lengthOfMonth();
                long daysStayed = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEnd) + 1;
                amount = amount.multiply(BigDecimal.valueOf(daysStayed))
                        .divide(BigDecimal.valueOf(daysInMonth), 2, java.math.RoundingMode.HALF_UP);
                chargeIsProRated = true;
            }

            billCharge.setAmount(amount);
            billCharge.setIsProRated(chargeIsProRated);
            billChargeRepository.save(billCharge);
        }
    }

    private void saveElectricityShare(Bill bill,
                                      ElectricityCalculationService.ElectricityCalculation calc,
                                      LocalDate periodStart, LocalDate periodEnd) {
        TenantElectricityShare share = new TenantElectricityShare();
        share.setBillId(bill.getId());
        share.setTenantId(bill.getTenantId());
        share.setRoomId(bill.getRoomId());
        share.setBillingPeriodStart(periodStart);
        share.setBillingPeriodEnd(periodEnd);
        share.setTotalRoomUnits(calc.totalRoomUnits());
        share.setActiveTenantCount(calc.totalRoomTenants());
        share.setTenantUnitShare(calc.tenantUnitShare());
        share.setElectricityCharge(calc.electricityCost());

        BigDecimal sharePercentage = calc.totalRoomUnits().compareTo(BigDecimal.ZERO) > 0
                ? calc.tenantUnitShare()
                .divide(calc.totalRoomUnits(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        share.setSharePercentage(sharePercentage);

        tenantElectricityShareRepository.save(share);
    }

    private BillResponse mapBillToResponse(Bill bill, User tenant, Room room, PG pg,
                                           PGBillingConfig config) {
        return BillResponse.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .tenantId(bill.getTenantId())
                .tenantName(tenant.getName())
                .tenantEmail(tenant.getEmail())
                .roomId(bill.getRoomId())
                .roomNumber(room.getRoomNumber())
                .pgId(bill.getPgId())
                .pgName(pg.getName())
                .billingPeriodStart(bill.getBillingPeriodStart())
                .billingPeriodEnd(bill.getBillingPeriodEnd())
                .roomRent(bill.getRoomRent())
                .electricityCharge(bill.getElectricityCharge())
                .otherCharges(bill.getOtherCharges())
                .discount(bill.getDiscount())
                .lateFee(bill.getLateFee())
                .additionalDeductions(bill.getAdditionalDeductions())
                .subtotal(bill.getSubtotal())
                .gstEnabled(bill.getGstEnabled())
                .gstPercentage(bill.getGstPercentage())
                .gstAmount(bill.getGstAmount())
                .securityDepositUsed(bill.getSecurityDepositUsed())
                .totalAmount(bill.getTotalAmount())
                .totalRoomUnits(bill.getTotalRoomUnits())
                .tenantUnitShare(bill.getTenantUnitShare())
                .totalRoomTenants(bill.getTotalRoomTenants())
                .status(bill.getStatus())
                .billType(bill.getBillType())
                .generationType(bill.getGenerationType())
                .dueDate(bill.getDueDate())
                .paidDate(bill.getPaidDate())
                .generatedDate(bill.getGeneratedDate())
                .paymentProofUrl(bill.getPaymentProofUrl())
                .paymentProofUploadedAt(bill.getPaymentProofUploadedAt())
                .paymentProofStatus(bill.getPaymentProofStatus())
                .paymentRejectionCount(bill.getPaymentRejectionCount())
                .lastRejectionReason(bill.getLastRejectionReason())
                .version(bill.getVersion())
                .notes(bill.getNotes())
                .upiPaymentLink(upiPaymentLinkService.generateUpiPaymentLink(bill, config))
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }
}

