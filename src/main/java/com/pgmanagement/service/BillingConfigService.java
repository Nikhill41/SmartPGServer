package com.pgmanagement.service;


import com.pgmanagement.constants.BillingErrorMessages;
import com.pgmanagement.constants.BillingSuccessMessages;
import com.pgmanagement.dto.response.BillingConfigResponse;
import com.pgmanagement.dto.request.CreateBillingConfigRequest;
import com.pgmanagement.dto.request.UpdateBillingConfigRequest;
import com.pgmanagement.entity.Owner;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.PGBillingConfig;
import com.pgmanagement.exception.DuplicateResourceException;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.OwnerRepository;
import com.pgmanagement.repository.PGBillingConfigRepository;
import com.pgmanagement.repository.PGRepository;
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
public class BillingConfigService {

    private final PGBillingConfigRepository billingConfigRepository;
    private final PGRepository pgRepository;
    private final OwnerRepository ownerRepository;




    /**
     * Create billing configuration for a PG
     */
    @Transactional
    public BillingConfigResponse createBillingConfig(Long ownerId, CreateBillingConfigRequest request) throws DuplicateResourceException {
        log.info("Creating billing config for PG ID: {} by owner ID: {}", request.getPgId(), ownerId);

        // Check if PG exists and owner has access
        PG pg = pgRepository.findById(request.getPgId())
                .orElseThrow(() -> new ResourceNotFoundException("PG not found with ID: " + request.getPgId()));

        if (!pg.getOwner().getId().equals(ownerId)) {
            log.error("Owner ID {} does not own PG ID {}", ownerId, request.getPgId());
            throw new ForbiddenException(BillingErrorMessages.NOT_PG_OWNER);
        }

        // Check if config already exists
        if (billingConfigRepository.existsByPgId(request.getPgId())) {
            log.error("Billing config already exists for PG ID: {}", request.getPgId());
            throw new DuplicateResourceException(
                    String.format(BillingErrorMessages.CONFIG_ALREADY_EXISTS, request.getPgId())
            );
        }

        // Create config
        PGBillingConfig config = new PGBillingConfig();
        config.setPgId(request.getPgId());
        config.setOwnerId(ownerId);

        mapRequestToEntity(request, config);

        config.setCreatedAt(LocalDateTime.now());

        PGBillingConfig savedConfig = billingConfigRepository.save(config);
        log.info("Billing config created successfully with ID: {}", savedConfig.getId());

        return mapEntityToResponse(savedConfig, pg);
    }

    /**
     * Get billing config for a PG
     */
    @Transactional(readOnly = true)
    public BillingConfigResponse getBillingConfig(Long ownerId, Long pgId) {
        log.info("Fetching billing config for PG ID: {} by owner ID: {}", pgId, ownerId);

        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new ResourceNotFoundException("PG not found with ID: " + pgId));

        if (!pg.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_PG_OWNER);
        }

        PGBillingConfig config = billingConfigRepository.findByPgId(pgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CONFIG_NOT_FOUND, pgId)
                ));

        return mapEntityToResponse(config, pg);
    }

    /**
     * Get all billing configs for owner
     */
    @Transactional(readOnly = true)
    public List<BillingConfigResponse> getAllBillingConfigs(Long ownerId) {
        log.info("Fetching all billing configs for owner ID: {}", ownerId);

        List<PGBillingConfig> configs = billingConfigRepository.findByOwnerId(ownerId);

        return configs.stream()
                .map(config -> {
                    PG pg = pgRepository.findById(config.getPgId()).orElse(null);
                    return mapEntityToResponse(config, pg);
                })
                .collect(Collectors.toList());
    }

    /**
     * Update billing configuration
     */
    @Transactional
    public BillingConfigResponse updateBillingConfig(Long ownerId, Long pgId, UpdateBillingConfigRequest request) {
        log.info("Updating billing config for PG ID: {} by owner ID: {}", pgId, ownerId);

        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new ResourceNotFoundException("PG not found with ID: " + pgId));

        if (!pg.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_PG_OWNER);
        }

        PGBillingConfig config = billingConfigRepository.findByPgId(pgId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(BillingErrorMessages.CONFIG_NOT_FOUND, pgId)
                ));

        // Update only non-null fields
        if (request.getUpiId() != null) config.setUpiId(request.getUpiId());
        if (request.getUpiName() != null) config.setUpiName(request.getUpiName());
        if (request.getBankAccountNumber() != null) config.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getBankIfscCode() != null) config.setBankIfscCode(request.getBankIfscCode());
        if (request.getBankAccountName() != null) config.setBankAccountName(request.getBankAccountName());
        if (request.getBankName() != null) config.setBankName(request.getBankName());
        if (request.getBankBranch() != null) config.setBankBranch(request.getBankBranch());

        if (request.getGstEnabled() != null) config.setGstEnabled(request.getGstEnabled());
        if (request.getGstNumber() != null) config.setGstNumber(request.getGstNumber());
        if (request.getGstPercentage() != null) config.setGstPercentage(request.getGstPercentage());

        if (request.getBillingMode() != null) config.setBillingMode(request.getBillingMode());
        if (request.getAutoGenerationDay() != null) config.setAutoGenerationDay(request.getAutoGenerationDay());
        if (request.getDueDaysOffset() != null) config.setDueDaysOffset(request.getDueDaysOffset());

        if (request.getElectricityRatePerUnit() != null) config.setElectricityRatePerUnit(request.getElectricityRatePerUnit());
        if (request.getIncludeElectricity() != null) config.setIncludeElectricity(request.getIncludeElectricity());
        if (request.getElectricitySplitMethod() != null) config.setElectricitySplitMethod(request.getElectricitySplitMethod());

        if (request.getLateFeeEnabled() != null) config.setLateFeeEnabled(request.getLateFeeEnabled());
        if (request.getLateFeeAmount() != null) config.setLateFeeAmount(request.getLateFeeAmount());
        if (request.getLateFeeDays() != null) config.setLateFeeDays(request.getLateFeeDays());

        if (request.getProRatedRentMode() != null) config.setProRatedRentMode(request.getProRatedRentMode());

        if (request.getSecurityDepositAmount() != null) config.setSecurityDepositAmount(request.getSecurityDepositAmount());
        if (request.getSecurityDepositDueDays() != null) config.setSecurityDepositDueDays(request.getSecurityDepositDueDays());

        if (request.getAutoApplyStandardCharges() != null) config.setAutoApplyStandardCharges(request.getAutoApplyStandardCharges());
        if (request.getProRateStandardCharges() != null) config.setProRateStandardCharges(request.getProRateStandardCharges());

        config.setUpdatedAt(LocalDateTime.now());

        PGBillingConfig updatedConfig = billingConfigRepository.save(config);
        log.info("Billing config updated successfully for PG ID: {}", pgId);

        return mapEntityToResponse(updatedConfig, pg);
    }

    /**
     * Delete billing configuration
     */
    @Transactional
    public void deleteBillingConfig(Long ownerId, Long pgId) {
        log.info("Deleting billing config for PG ID: {} by owner ID: {}", pgId, ownerId);

        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new ResourceNotFoundException("PG not found with ID: " + pgId));

        if (!pg.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException(BillingErrorMessages.NOT_PG_OWNER);
        }

        if (!billingConfigRepository.existsByPgId(pgId)) {
            throw new ResourceNotFoundException(
                    String.format(BillingErrorMessages.CONFIG_NOT_FOUND, pgId)
            );
        }

        billingConfigRepository.deleteByPgId(pgId);
        log.info("Billing config deleted successfully for PG ID: {}", pgId);
    }

    // ========== HELPER METHODS ==========

    private void mapRequestToEntity(CreateBillingConfigRequest request, PGBillingConfig entity) {
        entity.setUpiId(request.getUpiId());
        entity.setUpiName(request.getUpiName());
        entity.setBankAccountNumber(request.getBankAccountNumber());
        entity.setBankIfscCode(request.getBankIfscCode());
        entity.setBankAccountName(request.getBankAccountName());
        entity.setBankName(request.getBankName());
        entity.setBankBranch(request.getBankBranch());

        entity.setGstEnabled(request.getGstEnabled());
        entity.setGstNumber(request.getGstNumber());
        entity.setGstPercentage(request.getGstPercentage());

        entity.setBillingMode(request.getBillingMode());
        entity.setAutoGenerationDay(request.getAutoGenerationDay());
        entity.setDueDaysOffset(request.getDueDaysOffset());

        entity.setElectricityRatePerUnit(request.getElectricityRatePerUnit());
        entity.setIncludeElectricity(request.getIncludeElectricity());
        entity.setElectricitySplitMethod(request.getElectricitySplitMethod());

        entity.setLateFeeEnabled(request.getLateFeeEnabled());
        entity.setLateFeeAmount(request.getLateFeeAmount());
        entity.setLateFeeDays(request.getLateFeeDays());

        entity.setProRatedRentMode(request.getProRatedRentMode());

        entity.setSecurityDepositAmount(request.getSecurityDepositAmount());
        entity.setSecurityDepositDueDays(request.getSecurityDepositDueDays());

        entity.setAutoApplyStandardCharges(request.getAutoApplyStandardCharges());
        entity.setProRateStandardCharges(request.getProRateStandardCharges());
    }

    private BillingConfigResponse mapEntityToResponse(PGBillingConfig entity, PG pg) {
        return BillingConfigResponse.builder()
                .id(entity.getId())
                .pgId(entity.getPgId())
                .pgName(pg != null ? pg.getName() : null)
                .upiId(entity.getUpiId())
                .upiName(entity.getUpiName())
                .bankAccountNumber(entity.getBankAccountNumber())
                .bankIfscCode(entity.getBankIfscCode())
                .bankAccountName(entity.getBankAccountName())
                .bankName(entity.getBankName())
                .bankBranch(entity.getBankBranch())
                .gstEnabled(entity.getGstEnabled())
                .gstNumber(entity.getGstNumber())
                .gstPercentage(entity.getGstPercentage())
                .billingMode(entity.getBillingMode())
                .autoGenerationDay(entity.getAutoGenerationDay())
                .dueDaysOffset(entity.getDueDaysOffset())
                .electricityRatePerUnit(entity.getElectricityRatePerUnit())
                .includeElectricity(entity.getIncludeElectricity())
                .electricitySplitMethod(entity.getElectricitySplitMethod())
                .lateFeeEnabled(entity.getLateFeeEnabled())
                .lateFeeAmount(entity.getLateFeeAmount())
                .lateFeeDays(entity.getLateFeeDays())
                .proRatedRentMode(entity.getProRatedRentMode())
                .securityDepositAmount(entity.getSecurityDepositAmount())
                .securityDepositDueDays(entity.getSecurityDepositDueDays())
                .autoApplyStandardCharges(entity.getAutoApplyStandardCharges())
                .proRateStandardCharges(entity.getProRateStandardCharges())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}

