package com.pgmanagement.service;



import com.pgmanagement.constants.BillingConstants;
import com.pgmanagement.constants.BillingErrorMessages;
import com.pgmanagement.dto.response.BillResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.enums.BillStatus;
import com.pgmanagement.enums.BillType;
import com.pgmanagement.enums.GenerationType;
import com.pgmanagement.exception.DuplicateResourceException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityDepositService {

    private final BillRepository billRepository;
    private final PGBillingConfigRepository billingConfigRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final PGRepository pgRepository;
    private final BillNumberGeneratorService billNumberGeneratorService;
    private final UpiPaymentLinkService upiPaymentLinkService;
    private final EmailService emailService;

    /**
     * Generate security deposit bill (called when booking is accepted)
     */
    @Transactional
    public BillResponse generateSecurityDepositBill(Long tenantId, Long roomId, LocalDate moveInDate) throws DuplicateResourceException {
        log.info("Generating security deposit bill for tenant {} in room {}", tenantId, roomId);

        // Get room and PG
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        PG pg = room.getPg();

        // Get billing config
        PGBillingConfig config = billingConfigRepository.findByPgId(pg.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CONFIG_NOT_FOUND, pg.getId())));

        // Get tenant
        User tenant = userRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found with ID: " + tenantId));

        // Check if security deposit bill already exists
        List<Bill> existingDeposits = billRepository.findSecurityDepositBillsByTenantId(tenantId);
        if (!existingDeposits.isEmpty()) {
            // Check if any unpaid security deposit exists
            boolean hasUnpaidDeposit = existingDeposits.stream()
                    .anyMatch(bill -> bill.getStatus() == BillStatus.PENDING ||
                            bill.getStatus() == BillStatus.OVERDUE);
            if (hasUnpaidDeposit) {
                log.error("Unpaid security deposit bill already exists for tenant {}", tenantId);
                throw new DuplicateResourceException(BillingErrorMessages.SECURITY_DEPOSIT_EXISTS);
            }
        }

        // Create security deposit bill
        Bill bill = new Bill();
        bill.setBillNumber(billNumberGeneratorService.generateBillNumber(
                pg, room, BillType.SECURITY_DEPOSIT, LocalDate.now()));
        bill.setTenantId(tenantId);
        bill.setRoomId(roomId);
        bill.setPgId(pg.getId());
        bill.setOwnerId(pg.getOwner().getId());
        bill.setBillingPeriodStart(moveInDate);
        bill.setBillingPeriodEnd(moveInDate);
        bill.setRoomRent(BigDecimal.ZERO);
        bill.setElectricityCharge(BigDecimal.ZERO);
        bill.setOtherCharges(BigDecimal.ZERO);
        bill.setDiscount(BigDecimal.ZERO);
        bill.setLateFee(BigDecimal.ZERO);
        bill.setAdditionalDeductions(BigDecimal.ZERO);
        bill.setSubtotal(config.getSecurityDepositAmount());
        bill.setGstEnabled(false);
        bill.setGstPercentage(BigDecimal.ZERO);
        bill.setGstAmount(BigDecimal.ZERO);
        bill.setSecurityDepositUsed(BigDecimal.ZERO);
        bill.setTotalAmount(config.getSecurityDepositAmount());
        bill.setStatus(BillStatus.PENDING);
        bill.setBillType(BillType.SECURITY_DEPOSIT);
        bill.setGenerationType(GenerationType.AUTO);
        bill.setDueDate(moveInDate.plusDays(config.getSecurityDepositDueDays()));
        bill.setGeneratedDate(LocalDate.now());
        bill.setNotes("Security deposit for move-in");
        bill.setCreatedAt(LocalDateTime.now());

        Bill savedBill = billRepository.save(bill);
        log.info("Security deposit bill generated: {}", savedBill.getBillNumber());

        // Send email with payment details
        emailService.sendSecurityDepositBill(tenant, savedBill, config);

        return mapBillToResponse(savedBill, tenant, room, pg, config);
    }

    /**
     * Check if tenant has paid security deposit
     */
    @Transactional(readOnly = true)
    public boolean hasSecurityDepositPaid(Long tenantId) {
        List<Bill> securityDeposits = billRepository.findSecurityDepositBillsByTenantId(tenantId);

        boolean hasPaid = securityDeposits.stream()
                .anyMatch(bill -> bill.getStatus() == BillStatus.PAID);

        log.debug("Tenant {} security deposit paid status: {}", tenantId, hasPaid);
        return hasPaid;
    }

    /**
     * Get security deposit amount for refund calculation
     */
    @Transactional(readOnly = true)
    public BigDecimal getSecurityDepositAmount(Long tenantId) {
        List<Bill> securityDeposits = billRepository.findSecurityDepositBillsByTenantId(tenantId);

        return securityDeposits.stream()
                .filter(bill -> bill.getStatus() == BillStatus.PAID)
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ========== HELPER METHODS ==========

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
                .status(bill.getStatus())
                .billType(bill.getBillType())
                .generationType(bill.getGenerationType())
                .dueDate(bill.getDueDate())
                .paidDate(bill.getPaidDate())
                .generatedDate(bill.getGeneratedDate())
                .version(bill.getVersion())
                .notes(bill.getNotes())
                .upiPaymentLink(upiPaymentLinkService.generateUpiPaymentLink(bill, config))
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }
}

