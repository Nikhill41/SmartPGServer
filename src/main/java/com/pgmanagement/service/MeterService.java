package com.pgmanagement.service;

import com.pgmanagement.config.MeterProperties;
import com.pgmanagement.dto.request.PairMeterRequest;
import com.pgmanagement.dto.request.RelayCommandRequest;
import com.pgmanagement.dto.response.*;
import com.pgmanagement.entity.Meter;
import com.pgmanagement.entity.Room;
import com.pgmanagement.enums.MeterStatus;
import com.pgmanagement.enums.Role;
import com.pgmanagement.exception.MeterAlreadyPairedException;
import com.pgmanagement.exception.MeterOfflineException;
import com.pgmanagement.model.DeviceShadow;
import com.pgmanagement.model.MeterReading;
import com.pgmanagement.repository.MeterRepository;
import com.pgmanagement.service.aws.AwsIotShadowService;
import com.pgmanagement.service.aws.DynamoDbMeterReadingService;
import com.pgmanagement.util.TimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core service for meter operations
 * Handles pairing, live readings, history, and relay control
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeterService {

    private final MeterRepository meterRepository;
    private final MeterAuthorizationService authorizationService;
    private final AwsIotShadowService iotShadowService;
    private final DynamoDbMeterReadingService dynamoDbService;
    private final MeterProperties meterProperties;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Pair a meter to a room
     *
     * @param request Pairing request with meterId and roomId
     * @param ownerId Owner performing the pairing
     * @return PairMeterResponse with pairing details
     */
    @Transactional
    public PairMeterResponse pairMeter(PairMeterRequest request, Long ownerId) {
        log.info("Pairing meter {} to room {} by owner {}", request.getMeterId(), request.getRoomId(), ownerId);

        // 1. Verify owner owns the room
        Room room = authorizationService.verifyRoomOwnership(request.getRoomId(), ownerId);

        // 2. Check if room already has a meter
        if (authorizationService.roomHasMeter(request.getRoomId())) {
            log.warn("Room {} already has a meter paired", request.getRoomId());
            throw new MeterAlreadyPairedException("Room already has a meter paired");
        }

        // 3. Check if meter is already paired
        if (authorizationService.isMeterPaired(request.getMeterId())) {
            log.warn("Meter {} is already paired", request.getMeterId());
            throw new MeterAlreadyPairedException(request.getMeterId() + " is already paired to another room");
        }

        // 4. Create or update meter entity
        Meter meter = meterRepository.findByMeterId(request.getMeterId())
                .orElse(new Meter());

        meter.setMeterId(request.getMeterId());
        meter.setAwsThingName(request.getMeterId()); // AWS Thing Name = meterId
        meter.setRoom(room);
        meter.setOwnerId(ownerId);
        meter.setStatus(MeterStatus.ACTIVE);
        meter.setPairedAt(LocalDateTime.now());
        meter.setUnpairedAt(null);

        Meter savedMeter = meterRepository.save(meter);

        log.info("Meter {} successfully paired to room {}", request.getMeterId(), request.getRoomId());

        // 5. Build response
        return PairMeterResponse.builder()
                .success(true)
                .meterId(savedMeter.getMeterId())
                .awsThingName(savedMeter.getAwsThingName())
                .roomId(room.getId())
                .roomNumber(room.getRoomNumber())
                .ownerId(ownerId)
                .status(savedMeter.getStatus().name())
                .pairedAt(savedMeter.getPairedAt())
                .message("Meter successfully paired to room")
                .build();
    }

    /**
     * Unpair a meter from its room
     *
     * @param meterId Meter ID to unpair
     * @param ownerId Owner performing the unpairing
     * @return UnpairMeterResponse with unpairing details
     */
    @Transactional
    public UnpairMeterResponse unpairMeter(String meterId, Long ownerId) {
        log.info("Unpairing meter {} by owner {}", meterId, ownerId);

        // 1. Verify ownership
        Meter meter = authorizationService.verifyMeterOwnership(meterId, ownerId);

        // 2. Check if meter is paired
        if (!meter.isPaired()) {
            log.warn("Meter {} is already unpaired", meterId);
            throw new IllegalStateException("Meter is already unpaired");
        }

        // 3. Store room ID before unpairing
        Long previousRoomId = meter.getRoom().getId();

        // 4. Update meter status
        meter.setStatus(MeterStatus.UNPAIRED);
        meter.setUnpairedAt(LocalDateTime.now());
        // Note: We keep the room reference for historical data, but status indicates unpaired

        Meter savedMeter = meterRepository.save(meter);
        meterRepository.delete(meter);

        log.info("Meter {} successfully unpaired from room {}", meterId, previousRoomId);

        // 5. Build response
        return UnpairMeterResponse.builder()
                .success(true)
                .meterId(savedMeter.getMeterId())
                .roomId(previousRoomId)
                .status(savedMeter.getStatus().name())
                .unpairedAt(savedMeter.getUnpairedAt())
                .message("Meter successfully unpaired from room")
                .build();
    }

    /**
     * Get live meter readings by room ID
     *
     * @param roomId Room ID
     * @param userId Current user ID
     * @param userRole Current user role
     * @return LiveMeterResponse with current readings
     */
    public LiveMeterResponse getLiveByRoom(Long roomId, Long userId, Role userRole) {
        log.info("Fetching live readings for room {} by user {} ({})", roomId, userId, userRole);

        // 1. Verify access permission
        Meter meter = authorizationService.verifyCanViewMeterByRoom(roomId, userId, userRole);

        // 2. Fetch and return live data
        return fetchLiveData(meter);
    }

    /**
     * Get live meter readings by meter ID
     *
     * @param meterId Meter ID
     * @param userId Current user ID
     * @param userRole Current user role
     * @return LiveMeterResponse with current readings
     */
    public LiveMeterResponse getLiveByMeter(String meterId, Long userId, Role userRole) {
        log.info("Fetching live readings for meter {} by user {} ({})", meterId, userId, userRole);

        // 1. Verify access permission
        Meter meter = authorizationService.verifyCanViewMeter(meterId, userId, userRole);

        // 2. Fetch and return live data
        return fetchLiveData(meter);
    }

    /**
     * Get historical meter readings
     *
     * @param meterId Meter ID
     * @param startTimestamp Start time in Unix milliseconds
     * @param endTimestamp End time in Unix milliseconds
     * @param userId Current user ID
     * @param userRole Current user role
     * @return MeterHistoryResponse with readings and summary
     */
    public MeterHistoryResponse getHistory(String meterId, long startTimestamp, long endTimestamp,
                                           Long userId, Role userRole) {
        log.info("Fetching history for meter {} from {} to {} by user {} ({})",
                meterId, startTimestamp, endTimestamp, userId, userRole);

        // 1. Verify access permission
        Meter meter = authorizationService.verifyCanViewHistory(meterId, userId, userRole);

        // 2. Validate time range
        if (startTimestamp >= endTimestamp) {
            throw new IllegalArgumentException("Start timestamp must be before end timestamp");
        }

        // 3. Query DynamoDB for readings
        List<MeterReading> readings = dynamoDbService.queryReadings(meterId, startTimestamp, endTimestamp);

        log.info("Retrieved {} readings for meter {}", readings.size(), meterId);

        // 4. Build period info
        PeriodInfo periodInfo = buildPeriodInfo(startTimestamp, endTimestamp);

        // 5. Calculate summary statistics
        MeterHistorySummary summary = calculateSummary(readings);

        // 6. Convert readings to response format
        List<MeterReadingPoint> readingPoints = readings.stream()
                .map(this::convertToReadingPoint)
                .collect(Collectors.toList());

        // 7. Build response
        return MeterHistoryResponse.builder()
                .meterId(meter.getMeterId())
                .roomId(meter.getRoom().getId())
                .roomNumber(meter.getRoom().getRoomNumber())
                .period(periodInfo)
                .summary(summary)
                .readings(readingPoints)
                .build();
    }

    /**
     * Control relay (turn ON/OFF)
     *
     * @param meterId Meter ID
     * @param request Relay command request
     * @param ownerId Owner performing the control
     * @return RelayCommandResponse with command result
     */
    @Transactional
    public RelayCommandResponse controlRelay(String meterId, RelayCommandRequest request, Long ownerId) {
        log.info("Controlling relay for meter {}: {} by owner {}", meterId, request.getCommand(), ownerId);

        // 1. Verify control permission (owner only)
        Meter meter = authorizationService.verifyCanControlMeter(meterId, ownerId, Role.OWNER);

        // 2. Fetch shadow to check online status
        DeviceShadow shadow = iotShadowService.getDeviceShadow(meter.getAwsThingName());
        boolean isOnline = iotShadowService.isMeterOnline(shadow);

        if (!isOnline) {
            log.warn("Meter {} is offline, cannot control relay", meterId);
            long lastSeen = shadow.getReportedState() != null ? shadow.getReportedState().getTimestamp() : 0;
            long minutesAgo = TimeFormatter.getDifferenceInSeconds(lastSeen) / 60;
            throw new MeterOfflineException(meterId, minutesAgo);
        }

        // 3. Publish MQTT command
        String commandId = iotShadowService.publishRelayCommand(meterId, request.getCommand());
        long commandTimestamp = System.currentTimeMillis();

        // 4. Verify relay status change (optional but recommended)
        boolean verified = iotShadowService.verifyRelayStatus(meter.getAwsThingName(), request.getCommand());

        // 5. Fetch final status from shadow
        DeviceShadow updatedShadow = iotShadowService.getDeviceShadow(meter.getAwsThingName());
        String finalStatus = updatedShadow.hasReportedData() ?
                updatedShadow.getReportedState().getRelayStatus() : "UNKNOWN";

        log.info("Relay command for meter {}: requested={}, final={}, verified={}",
                meterId, request.getCommand(), finalStatus, verified);

        // 6. Build response
        return RelayCommandResponse.builder()
                .success(true)
                .meterId(meterId)
                .requestedStatus(request.getCommand())
                .finalStatus(finalStatus)
                .commandId(commandId)
                .updatedAt(commandTimestamp)
                .verified(verified)
                .message(verified ?
                        "Relay command executed and verified successfully" :
                        "Relay command sent but verification timed out")
                .build();
    }

    /**
     * Get all meters for an owner
     *
     * @param ownerId Owner ID
     * @return List of basic meter info
     */
    public List<MeterBasicInfoDTO> getOwnerMeters(Long ownerId) {
        log.info("Fetching all meters for owner {}", ownerId);

        List<Meter> meters = meterRepository.findAllByOwnerId(ownerId);

        return meters.stream()
                .map(this::convertToBasicInfo)
                .collect(Collectors.toList());
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Fetch live data from AWS IoT Shadow
     */
    private LiveMeterResponse fetchLiveData(Meter meter) {
        try {
            // Fetch shadow
            DeviceShadow shadow = iotShadowService.getDeviceShadow(meter.getAwsThingName());

            // Check if shadow has data
            if (!shadow.hasReportedData()) {
                log.warn("Shadow for meter {} has no reported data", meter.getMeterId());
                return buildOfflineResponse(meter);
            }

            DeviceShadow.Reported reported = shadow.getReportedState();

            // Check online status
            boolean isOnline = iotShadowService.isMeterOnline(shadow);
            String displayTimestamp = iotShadowService.getLastSeenDisplay(shadow);

            // Build readings
            LiveReadings readings = LiveReadings.builder()
                    .voltage(reported.getVoltage())
                    .current(reported.getCurrent())
                    .power(reported.getPower())
                    .energy(reported.getEnergy())
                    .frequency(reported.getFrequency())
                    .powerFactor(reported.getPowerFactor())
                    .build();

            // Build response
            return LiveMeterResponse.builder()
                    .meterId(meter.getMeterId())
                    .roomId(meter.getRoom().getId())
                    .roomNumber(meter.getRoom().getRoomNumber())
                    .roomType(meter.getRoom().getType().name())
                    .isOnline(isOnline)
                    .lastSeen(reported.getTimestamp())
                    .displayTimestamp(displayTimestamp)
                    .readings(readings)
                    .relayStatus(reported.getRelayStatus())
                    .build();

        } catch (Exception e) {
            log.error("Error fetching live data for meter {}: {}", meter.getMeterId(), e.getMessage(), e);
            return buildOfflineResponse(meter);
        }
    }

    /**
     * Build offline response when shadow is unavailable
     */
    private LiveMeterResponse buildOfflineResponse(Meter meter) {
        return LiveMeterResponse.builder()
                .meterId(meter.getMeterId())
                .roomId(meter.getRoom().getId())
                .roomNumber(meter.getRoom().getRoomNumber())
                .roomType(meter.getRoom().getType().name())
                .isOnline(false)
                .lastSeen(null)
                .displayTimestamp("never")
                .readings(null)
                .relayStatus("UNKNOWN")
                .build();
    }

    /**
     * Build period info from timestamps
     */
    private PeriodInfo buildPeriodInfo(long startTimestamp, long endTimestamp) {
        LocalDateTime startDt = TimeFormatter.toLocalDateTime(startTimestamp);
        LocalDateTime endDt = TimeFormatter.toLocalDateTime(endTimestamp);

        String startIso = startDt.format(ISO_FORMATTER);
        String endIso = endDt.format(ISO_FORMATTER);

        long durationSeconds = (endTimestamp - startTimestamp) / 1000;
        String duration = formatDuration(durationSeconds);

        return PeriodInfo.builder()
                .startTimestamp(startTimestamp)
                .endTimestamp(endTimestamp)
                .startIso(startIso)
                .endIso(endIso)
                .duration(duration)
                .build();
    }

    /**
     * Calculate summary statistics from readings
     */
    private MeterHistorySummary calculateSummary(List<MeterReading> readings) {
        if (readings.isEmpty()) {
            return MeterHistorySummary.builder()
                    .totalReadings(0)
                    .build();
        }

        double sumVoltage = 0, sumCurrent = 0, sumPower = 0, sumFrequency = 0, sumPowerFactor = 0;
        double peakPower = 0;
        long peakPowerTimestamp = 0;

        for (MeterReading reading : readings) {
            sumVoltage += reading.getVoltage() != null ? reading.getVoltage() : 0;
            sumCurrent += reading.getCurrent() != null ? reading.getCurrent() : 0;
            sumPower += reading.getPower() != null ? reading.getPower() : 0;
            sumFrequency += reading.getFrequency() != null ? reading.getFrequency() : 0;
            sumPowerFactor += reading.getPowerFactor() != null ? reading.getPowerFactor() : 0;

            if (reading.getPower() != null && reading.getPower() > peakPower) {
                peakPower = reading.getPower();
                peakPowerTimestamp = reading.getTimestamp();
            }
        }

        int count = readings.size();
        Double energyStart = readings.get(0).getEnergy();
        Double energyEnd = readings.get(count - 1).getEnergy();
        Double energyConsumed = (energyStart != null && energyEnd != null) ?
                (energyEnd - energyStart) : null;

        return MeterHistorySummary.builder()
                .totalReadings(count)
                .avgVoltage(round(sumVoltage / count, 2))
                .avgCurrent(round(sumCurrent / count, 2))
                .avgPower(round(sumPower / count, 2))
                .peakPower(round(peakPower, 2))
                .peakPowerTimestamp(peakPowerTimestamp)
                .energyStart(energyStart)
                .energyEnd(energyEnd)
                .energyConsumed(energyConsumed != null ? round(energyConsumed, 4) : null)
                .avgFrequency(round(sumFrequency / count, 2))
                .avgPowerFactor(round(sumPowerFactor / count, 3))
                .build();
    }

    /**
     * Convert MeterReading to MeterReadingPoint DTO
     */
    private MeterReadingPoint convertToReadingPoint(MeterReading reading) {
        LocalDateTime dt = TimeFormatter.toLocalDateTime(reading.getTimestamp());
        String timestampIso = dt.format(ISO_FORMATTER);

        return MeterReadingPoint.builder()
                .timestamp(reading.getTimestamp())
                .timestampIso(timestampIso)
                .voltage(reading.getVoltage())
                .current(reading.getCurrent())
                .power(reading.getPower())
                .energy(reading.getEnergy())
                .frequency(reading.getFrequency())
                .powerFactor(reading.getPowerFactor())
                .relayStatus(reading.getRelayStatus())
                .build();
    }

    /**
     * Convert Meter entity to basic info DTO
     */
    private MeterBasicInfoDTO convertToBasicInfo(Meter meter) {
        // Optionally fetch online status (may be expensive for large lists)
        Boolean isOnline = null;
        String lastSeen = null;

        try {
            DeviceShadow shadow = iotShadowService.getDeviceShadow(meter.getAwsThingName());
            isOnline = iotShadowService.isMeterOnline(shadow);
            lastSeen = iotShadowService.getLastSeenDisplay(shadow);
        } catch (Exception e) {
            log.debug("Could not fetch shadow for meter {} in list view", meter.getMeterId());
        }

        return MeterBasicInfoDTO.builder()
                .meterId(meter.getMeterId())
                .roomId(meter.getRoom().getId())
                .roomNumber(meter.getRoom().getRoomNumber())
                .pgName(meter.getRoom().getPg().getName())
                .status(meter.getStatus().name())
                .isOnline(isOnline)
                .lastSeen(lastSeen)
                .pairedAt(meter.getPairedAt())
                .build();
    }

    /**
     * Format duration in seconds to human-readable string
     */
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;

        if (hours > 24) {
            long days = hours / 24;
            return days + (days == 1 ? " day" : " days");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour" : " hours");
        } else {
            return minutes + (minutes == 1 ? " minute" : " minutes");
        }
    }

    /**
     * Round double to specified decimal places
     */
    private Double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
