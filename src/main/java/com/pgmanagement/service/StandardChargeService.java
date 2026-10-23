package com.pgmanagement.service;



import com.pgmanagement.constants.BillingErrorMessages;
import com.pgmanagement.constants.BillingSuccessMessages;
import com.pgmanagement.dto.request.CreateStandardChargeRequest;
import com.pgmanagement.dto.response.StandardChargeResponse;
import com.pgmanagement.dto.request.UpdateStandardChargeRequest;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.StandardCharge;
import com.pgmanagement.exception.DuplicateResourceException;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.PGRepository;
import com.pgmanagement.repository.StandardChargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StandardChargeService {

    private final StandardChargeRepository standardChargeRepository;
    private final PGRepository pgRepository;

    /**
     * Create standard charge
     */
    @Transactional
    public StandardChargeResponse createStandardCharge(Long ownerId, CreateStandardChargeRequest request) throws DuplicateResourceException {
        log.info("Creating standard charge '{}' for PG ID: {} by owner ID: {}",
                request.getChargeName(), request.getPgId(), ownerId);

        // Verify PG ownership
        PG pg = verifyPGOwnership(ownerId, request.getPgId());

        // Check for duplicate charge name
        if (standardChargeRepository.existsByPgIdAndChargeName(
                request.getPgId(), request.getChargeName(), null)) {
            log.error("Standard charge '{}' already exists for PG ID: {}",
                    request.getChargeName(), request.getPgId());
            throw new DuplicateResourceException(
                    String.format(BillingErrorMessages.CHARGE_NAME_DUPLICATE, request.getChargeName())
            );
        }

        // Create charge
        StandardCharge charge = new StandardCharge();
        charge.setOwnerId(ownerId);
        charge.setPgId(request.getPgId());
        charge.setChargeName(request.getChargeName());
        charge.setAmount(request.getAmount());
        charge.setIsActive(request.getIsActive());
        charge.setIsDefault(request.getIsDefault());
        charge.setIsDeleted(false);
        charge.setCreatedAt(LocalDateTime.now());

        StandardCharge savedCharge = standardChargeRepository.save(charge);
        log.info("Standard charge created successfully with ID: {}", savedCharge.getId());

        return mapEntityToResponse(savedCharge, pg);
    }

    /**
     * Get all standard charges for a PG
     */
    @Transactional(readOnly = true)
    public List<StandardChargeResponse> getStandardCharges(Long ownerId, Long pgId) {
        log.info("Fetching standard charges for PG ID: {} by owner ID: {}", pgId, ownerId);

        PG pg = verifyPGOwnership(ownerId, pgId);

        List<StandardCharge> charges = standardChargeRepository.findByPgIdAndIsDeletedFalse(pgId);

        return charges.stream()
                .map(charge -> mapEntityToResponse(charge, pg))
                .collect(Collectors.toList());
    }

    /**
     * Get active standard charges (for billing)
     */
    @Transactional(readOnly = true)
    public List<StandardChargeResponse> getActiveStandardCharges(Long pgId) {
        log.info("Fetching active standard charges for PG ID: {}", pgId);

        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new ResourceNotFoundException("PG not found with ID: " + pgId));

        List<StandardCharge> charges = standardChargeRepository.findActiveBypgId(pgId);

        return charges.stream()
                .map(charge -> mapEntityToResponse(charge, pg))
                .collect(Collectors.toList());
    }

    /**
     * Update standard charge
     */
    @Transactional
    public StandardChargeResponse updateStandardCharge(
            Long ownerId, Long pgId, Long chargeId, UpdateStandardChargeRequest request) throws DuplicateResourceException {
        log.info("Updating standard charge ID: {} for PG ID: {} by owner ID: {}",
                chargeId, pgId, ownerId);

        PG pg = verifyPGOwnership(ownerId, pgId);

        StandardCharge charge = standardChargeRepository.findById(chargeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CHARGE_NOT_FOUND, chargeId)
                ));

        // Verify charge belongs to PG
        if (!charge.getPgId().equals(pgId)) {
            throw new ForbiddenException("Standard charge does not belong to this PG");
        }

        // Check for duplicate name if changing
        if (request.getChargeName() != null && !request.getChargeName().equals(charge.getChargeName())) {
            if (standardChargeRepository.existsByPgIdAndChargeName(
                    pgId, request.getChargeName(), chargeId)) {
                throw new DuplicateResourceException(
                        String.format(BillingErrorMessages.CHARGE_NAME_DUPLICATE, request.getChargeName())
                );
            }
            charge.setChargeName(request.getChargeName());
        }

        // Update fields
        if (request.getAmount() != null) charge.setAmount(request.getAmount());
        if (request.getIsActive() != null) charge.setIsActive(request.getIsActive());
        if (request.getIsDefault() != null) charge.setIsDefault(request.getIsDefault());

        charge.setUpdatedAt(LocalDateTime.now());

        StandardCharge updatedCharge = standardChargeRepository.save(charge);
        log.info("Standard charge updated successfully: {}", chargeId);

        return mapEntityToResponse(updatedCharge, pg);
    }

    /**
     * Toggle charge active status
     */
    @Transactional
    public StandardChargeResponse toggleChargeActive(Long ownerId, Long pgId, Long chargeId) {
        log.info("Toggling active status for charge ID: {} in PG ID: {}", chargeId, pgId);

        PG pg = verifyPGOwnership(ownerId, pgId);

        StandardCharge charge = standardChargeRepository.findById(chargeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CHARGE_NOT_FOUND, chargeId)
                ));

        if (!charge.getPgId().equals(pgId)) {
            throw new ForbiddenException("Standard charge does not belong to this PG");
        }

        charge.setIsActive(!charge.getIsActive());
        charge.setUpdatedAt(LocalDateTime.now());

        StandardCharge updatedCharge = standardChargeRepository.save(charge);
        log.info("Standard charge active status toggled to: {}", updatedCharge.getIsActive());

        return mapEntityToResponse(updatedCharge, pg);
    }

    /**
     * Soft delete standard charge
     */
    @Transactional
    public void deleteStandardCharge(Long ownerId, Long pgId, Long chargeId) {
        log.info("Soft deleting standard charge ID: {} in PG ID: {}", chargeId, pgId);

        verifyPGOwnership(ownerId, pgId);

        StandardCharge charge = standardChargeRepository.findById(chargeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CHARGE_NOT_FOUND, chargeId)
                ));

        if (!charge.getPgId().equals(pgId)) {
            throw new ForbiddenException("Standard charge does not belong to this PG");
        }

        charge.setIsDeleted(true);
        charge.setUpdatedAt(LocalDateTime.now());

        standardChargeRepository.save(charge);
        log.info("Standard charge soft deleted successfully: {}", chargeId);
    }

    // ========== HELPER METHODS ==========

    private PG verifyPGOwnership(Long ownerId, Long pgId) {
        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new ResourceNotFoundException("PG not found with ID: " + pgId));

        if (!pg.getOwner().getId().equals(ownerId)) {
            log.error("Owner ID {} does not own PG ID {}", ownerId, pgId);
            throw new ForbiddenException(BillingErrorMessages.NOT_PG_OWNER);
        }

        return pg;
    }

    private StandardChargeResponse mapEntityToResponse(StandardCharge entity, PG pg) {
        return StandardChargeResponse.builder()
                .id(entity.getId())
                .pgId(entity.getPgId())
                .pgName(pg != null ? pg.getName() : null)
                .chargeName(entity.getChargeName())
                .amount(entity.getAmount())
                .isActive(entity.getIsActive())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

