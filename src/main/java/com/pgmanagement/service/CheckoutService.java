package com.pgmanagement.service;



import com.pgmanagement.constants.BillingErrorMessages;

import com.pgmanagement.dto.request.AdditionalDeductionRequest;
import com.pgmanagement.dto.request.InitiateCheckoutRequest;
import com.pgmanagement.dto.request.ProcessCheckoutRequest;
import com.pgmanagement.dto.request.SettlementCompleteRequest;
import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.dto.response.CheckoutPreviewResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.enums.*;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.IllegalStateException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final TenantStayRecordRepository tenantStayRecordRepository;
    private final BillRepository billRepository;
    private final PGBillingConfigRepository billingConfigRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PGRepository pgRepository;
    private final StandardChargeRepository standardChargeRepository;

    private final BillCalculationService billCalculationService;
    private final ElectricityCalculationService electricityCalculationService;
    private final BillNumberGeneratorService billNumberGeneratorService;
    private final SecurityDepositService securityDepositService;
    private final EmailService emailService;

    /**
     * Initiate checkout request by tenant
     */
    @Transactional
    public void initiateCheckout(Long tenantId, InitiateCheckoutRequest request) throws IllegalStateException {
        log.info("Tenant {} initiating checkout with move-out date {}", tenantId, request.getMoveOutDate());

        // Get active stay
        TenantStayRecord stay = tenantStayRecordRepository
                .findByTenantIdAndStayStatus(tenantId, StayStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(BillingErrorMessages.NO_ACTIVE_STAY));

        // Validate move-out date
        if (request.getMoveOutDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(BillingErrorMessages.CHECKOUT_DATE_INVALID);
        }

        if (stay.getCheckoutInitiated()) {
            throw new IllegalStateException(BillingErrorMessages.CHECKOUT_ALREADY_INITIATED);
        }

        // Update stay record
        stay.setCheckoutInitiated(true);
        stay.setIntendedMoveOutDate(request.getMoveOutDate());
        stay.setUpdatedAt(LocalDateTime.now());
        tenantStayRecordRepository.save(stay);

        log.info("Checkout initiated for tenant {}, intended move-out: {}",
                tenantId, request.getMoveOutDate());

        // Notify owner
        User tenant = userRepository.findById(tenantId).orElse(null);
        emailService.sendCheckoutRequest(tenant, stay, request);
    }

    /**
     * Get checkout preview for tenant
     */
    @Transactional(readOnly = true)
    public CheckoutPreviewResponse getCheckoutPreview(Long tenantId) {
        log.info("Generating checkout preview for tenant {}", tenantId);

        TenantStayRecord stay = tenantStayRecordRepository
                .findByTenantIdAndStayStatus(tenantId, StayStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(BillingErrorMessages.NO_ACTIVE_STAY));

        User tenant = userRepository.findById(tenantId).orElse(null);
        Room room = roomRepository.findById(stay.getRoomId()).orElse(null);

        LocalDate moveOutDate = stay.getIntendedMoveOutDate() != null
                ? stay.getIntendedMoveOutDate()
                : LocalDate.now().plusDays(7);

        // Get pending bills
        List<Bill> pendingBills = billRepository.findByTenantIdAndStatus(tenantId, BillStatus.PENDING);
        pendingBills.addAll(billRepository.findByTenantIdAndStatus(tenantId, BillStatus.OVERDUE));

        BigDecimal pendingBillsAmount = pendingBills.stream()
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate preview amounts (will be accurate in actual checkout)
        int daysStayed = (int) ChronoUnit.DAYS.between(stay.getMoveInDate(), moveOutDate);
        BigDecimal securityDepositPaid = securityDepositService.getSecurityDepositAmount(tenantId);

        return CheckoutPreviewResponse.builder()
                .tenantId(tenantId)
                .tenantName(tenant != null ? tenant.getName() : null)
                .roomId(stay.getRoomId())
                .roomNumber(room != null ? room.getRoomNumber() : null)
                .moveInDate(stay.getMoveInDate())
                .intendedMoveOutDate(moveOutDate)
                .daysStayed(daysStayed)
                .pendingBills(pendingBills.stream()
                        .map(bill -> mapBillToSimpleResponse(bill))
                        .collect(Collectors.toList()))
                .pendingBillsAmount(pendingBillsAmount)
                .securityDepositPaid(securityDepositPaid)
                .build();
    }

    /**
     * Process checkout by owner (generate final settlement bill)
     */
    @Transactional
    public BillResponse processCheckout(Long ownerId, Long tenantId, ProcessCheckoutRequest request) {
        log.info("Owner {} processing checkout for tenant {} with move-out date {}",
                ownerId, tenantId, request.getMoveOutDate());

        // Get stay record
        TenantStayRecord stay = tenantStayRecordRepository
                .findByTenantIdAndStayStatus(tenantId, StayStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(BillingErrorMessages.NO_ACTIVE_STAY));

        Room room = roomRepository.findById(stay.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        PG pg = room.getPg();

        // Verify ownership
        if (!pg.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_PG_OWNER);
        }

        PGBillingConfig config = billingConfigRepository.findByPgId(pg.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CONFIG_NOT_FOUND, pg.getId())));

        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        LocalDate moveOutDate = request.getMoveOutDate();

        // Cancel any full-month bills for the checkout month
        LocalDate currentMonthStart = moveOutDate.withDayOfMonth(1);
        billRepository.cancelBillsForPeriod(
                tenantId, currentMonthStart, moveOutDate.plusDays(1),
                "Tenant checked out mid-month - replaced with final settlement");

        // Calculate pro-rated charges
        BigDecimal roomRent = billCalculationService.calculateRent(
                room, currentMonthStart, moveOutDate, config, true);

        ElectricityCalculationService.ElectricityCalculation electricityCalc =
                electricityCalculationService.calculateElectricity(
                        room, tenantId, currentMonthStart, moveOutDate, config);

        List<StandardCharge> standardCharges = standardChargeRepository.findActiveDefaultByPgId(pg.getId());
        BigDecimal otherCharges = billCalculationService.calculateOtherCharges(
                standardCharges, currentMonthStart, moveOutDate, config, true);

        // Calculate additional deductions
        BigDecimal additionalDeductions = BigDecimal.ZERO;
        if (request.getAdditionalDeductions() != null && !request.getAdditionalDeductions().isEmpty()) {
            additionalDeductions = request.getAdditionalDeductions().stream()
                    .map(AdditionalDeductionRequest::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Calculate totals
        BillCalculationService.BillTotals totals = billCalculationService.calculateBillTotals(
                roomRent,
                electricityCalc.electricityCost(),
                otherCharges,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                additionalDeductions,
                config
        );

        // Calculate security deposit adjustment
        BigDecimal totalDues = totals.totalAmount();
        BigDecimal securityDeposit = securityDepositService.getSecurityDepositAmount(tenantId);
        BigDecimal securityDepositUsed = BigDecimal.ZERO;
        BigDecimal finalAmount = totalDues;

        if (request.getDeductSecurityDeposit() != null && request.getDeductSecurityDeposit()) {
            if (totalDues.compareTo(securityDeposit) <= 0) {
                securityDepositUsed = totalDues;
                finalAmount = BigDecimal.ZERO;
            } else {
                securityDepositUsed = securityDeposit;
                finalAmount = totalDues.subtract(securityDeposit);
            }
        }

        // Create final settlement bill
        Bill finalBill = new Bill();
        finalBill.setBillNumber(billNumberGeneratorService.generateBillNumber(
                pg, room, BillType.FINAL_SETTLEMENT, LocalDate.now()));
        finalBill.setTenantId(tenantId);
        finalBill.setRoomId(room.getId());
        finalBill.setPgId(pg.getId());
        finalBill.setOwnerId(ownerId);
        finalBill.setBillingPeriodStart(currentMonthStart);
        finalBill.setBillingPeriodEnd(moveOutDate);
        finalBill.setRoomRent(roomRent);
        finalBill.setElectricityCharge(electricityCalc.electricityCost());
        finalBill.setOtherCharges(otherCharges);
        finalBill.setDiscount(BigDecimal.ZERO);
        finalBill.setLateFee(BigDecimal.ZERO);
        finalBill.setAdditionalDeductions(additionalDeductions);
        finalBill.setSubtotal(totals.subtotal());
        finalBill.setGstEnabled(config.getGstEnabled());
        finalBill.setGstPercentage(config.getGstPercentage());
        finalBill.setGstAmount(totals.gstAmount());
        finalBill.setSecurityDepositUsed(securityDepositUsed);
        finalBill.setTotalAmount(finalAmount);
        finalBill.setTotalRoomUnits(electricityCalc.totalRoomUnits());
        finalBill.setTenantUnitShare(electricityCalc.tenantUnitShare());
        finalBill.setTotalRoomTenants(electricityCalc.totalRoomTenants());
        finalBill.setStatus(BillStatus.PENDING);
        finalBill.setBillType(BillType.FINAL_SETTLEMENT);
        finalBill.setGenerationType(GenerationType.MANUAL);
        finalBill.setDueDate(moveOutDate.plusDays(2));
        finalBill.setGeneratedDate(LocalDate.now());
        finalBill.setNotes(request.getNotes());
        finalBill.setCreatedAt(LocalDateTime.now());

        Bill savedBill = billRepository.save(finalBill);

        // Update stay record
        stay.setMoveOutDate(moveOutDate);
        stay.setStayStatus(StayStatus.PENDING_SETTLEMENT);
        stay.setDaysStayed((int) ChronoUnit.DAYS.between(stay.getMoveInDate(), moveOutDate));
        stay.setUpdatedAt(LocalDateTime.now());
        tenantStayRecordRepository.save(stay);

        log.info("Final settlement bill generated: {}", savedBill.getBillNumber());

        // Send final bill email
        emailService.sendFinalSettlementBill(savedBill, config);

        return mapBillToResponse(savedBill, tenant, room, pg, config);
    }

    /**
     * Mark checkout as complete (after final bill is paid)
     */
    @Transactional
    public void completeCheckout(Long ownerId, Long billId, SettlementCompleteRequest request) throws IllegalStateException {
        log.info("Owner {} marking checkout complete for bill {}", ownerId, billId);

        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.BILL_NOT_FOUND, billId)));

        if (!bill.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_BILL_OWNER);
        }

        if (bill.getBillType() != BillType.FINAL_SETTLEMENT) {
            throw new IllegalStateException("Not a final settlement bill");
        }

        // Mark bill as settled
        bill.setStatus(BillStatus.SETTLED);
        bill.setPaidDate(request.getSettlementDate());
        bill.setUpdatedAt(LocalDateTime.now());
        billRepository.save(bill);

        // Update stay record
        TenantStayRecord stay = tenantStayRecordRepository
                .findByTenantIdAndStayStatus(bill.getTenantId(), StayStatus.PENDING_SETTLEMENT)
                .orElseThrow(() -> new ResourceNotFoundException("Stay record not found"));

        stay.setStayStatus(StayStatus.COMPLETED);
        stay.setCheckoutCompletedAt(LocalDateTime.now());
        tenantStayRecordRepository.save(stay);

        // ✅ Update User record — detach from room
        User tenant = userRepository.findById(bill.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
        tenant.setRoom(null);
        tenant.setIsActive(false);
        tenant.setMoveInDate(null);
        userRepository.save(tenant);
        log.info("User {} detached from room after checkout complete", bill.getTenantId());

        // ✅ Update Room occupancy
        Room room = roomRepository.findById(stay.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        room.decrementOccupancy();
        roomRepository.save(room);
        log.info("Room {} occupancy decremented after checkout complete", stay.getRoomId());

        log.info("Checkout completed for tenant {}", bill.getTenantId());


        // Send confirmation email
        emailService.sendCheckoutConfirmation(bill, request);
    }

    // ========== HELPER METHODS ==========

    private BillResponse mapBillToSimpleResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .totalAmount(bill.getTotalAmount())
                .dueDate(bill.getDueDate())
                .status(bill.getStatus())
                .billType(bill.getBillType())
                .build();
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
                .version(bill.getVersion())
                .notes(bill.getNotes())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }
}

