package com.pgmanagement.service;

import com.pgmanagement.entity.BookingRequest;
import com.pgmanagement.entity.Meter;
import com.pgmanagement.entity.Room;
import com.pgmanagement.enums.BookingStatus;
import com.pgmanagement.enums.Role;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.MeterNotFoundException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.exception.UnauthorizedMeterAccessException;
import com.pgmanagement.repository.BookingRequestRepository;
import com.pgmanagement.repository.MeterRepository;
import com.pgmanagement.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for meter access authorization and permission checks
 * Centralizes all permission logic for meter operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeterAuthorizationService {

    private final MeterRepository meterRepository;
    private final RoomRepository roomRepository;
    private final BookingRequestRepository bookingRequestRepository;

    /**
     * Verify user can view meter (read access)
     * Allowed for:
     * - Owner of the meter
     * - Tenant with active booking in that room
     *
     * @param meterId Meter ID
     * @param userId Current user ID
     * @param userRole Current user role
     * @throws UnauthorizedMeterAccessException if access denied
     * @return Meter entity if authorized
     */
    public Meter verifyCanViewMeter(String meterId, Long userId, Role userRole) {
        log.debug("Verifying view access for meter {} by user {} with role {}", meterId, userId, userRole);

        Meter meter = meterRepository.findByMeterId(meterId)
                .orElseThrow(() -> new MeterNotFoundException(meterId));

        // Check if user is the owner
        if (meter.getOwnerId().equals(userId)) {
            log.debug("Access granted: User {} is owner of meter {}", userId, meterId);
            return meter;
        }

        // Check if user is tenant in that room
        if (userRole == Role.USER) {
            boolean isTenant = isActiveTenantInRoom(userId, meter.getRoom().getId());
            if (isTenant) {
                log.debug("Access granted: User {} is active tenant in room {}", userId, meter.getRoom().getId());
                return meter;
            }
        }

        log.warn("Access denied: User {} cannot view meter {}", userId, meterId);
        throw new UnauthorizedMeterAccessException(meterId, userId);
    }

    /**
     * Verify user can view meter by room ID
     *
     * @param roomId Room ID
     * @param userId Current user ID
     * @param userRole Current user role
     * @throws UnauthorizedMeterAccessException if access denied
     * @return Meter entity if authorized
     */
    public Meter verifyCanViewMeterByRoom(Long roomId, Long userId, Role userRole) {
        log.debug("Verifying view access for room {} meter by user {} with role {}", roomId, userId, userRole);

        // Find meter for this room
        Meter meter = meterRepository.findByRoomId(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Meter", "roomId", roomId));

        // Check if user is the owner
        if (meter.getOwnerId().equals(userId)) {
            log.debug("Access granted: User {} is owner of meter in room {}", userId, roomId);
            return meter;
        }

        // Check if user is tenant in that room
        if (userRole == Role.USER) {
            boolean isTenant = isActiveTenantInRoom(userId, roomId);
            if (isTenant) {
                log.debug("Access granted: User {} is active tenant in room {}", userId, roomId);
                return meter;
            }
        }

        log.warn("Access denied: User {} cannot view meter in room {}", userId, roomId);
        throw new UnauthorizedMeterAccessException();
    }

    /**
     * Verify user can control meter (write access)
     * Only owners are allowed to control meters
     *
     * @param meterId Meter ID
     * @param userId Current user ID
     * @param userRole Current user role
     * @throws ForbiddenException if user is not owner
     * @return Meter entity if authorized
     */
    public Meter verifyCanControlMeter(String meterId, Long userId, Role userRole) {
        log.debug("Verifying control access for meter {} by user {} with role {}", meterId, userId, userRole);

        // Only owners can control meters
        if (userRole != Role.OWNER) {
            log.warn("Control denied: User {} is not an owner", userId);
            throw new ForbiddenException("Only owners can control meters");
        }

        Meter meter = meterRepository.findByMeterId(meterId)
                .orElseThrow(() -> new MeterNotFoundException(meterId));

        // Verify ownership
        if (!meter.getOwnerId().equals(userId)) {
            log.warn("Control denied: User {} does not own meter {}", userId, meterId);
            throw new ForbiddenException("You don't have permission to control this meter");
        }

        log.debug("Control access granted for meter {} to user {}", meterId, userId);
        return meter;
    }

    /**
     * Verify user owns the room (for pairing/unpairing operations)
     *
     * @param roomId Room ID
     * @param ownerId Owner ID
     * @throws ForbiddenException if owner doesn't own the room
     * @return Room entity if authorized
     */
    public Room verifyRoomOwnership(Long roomId, Long ownerId) {
        log.debug("Verifying room {} ownership by owner {}", roomId, ownerId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        // Check if room belongs to owner's PG
        if (!room.getPg().getOwner().getId().equals(ownerId)) {
            log.warn("Room ownership denied: Owner {} does not own room {}", ownerId, roomId);
            throw new ForbiddenException("You don't have permission to manage this room");
        }

        log.debug("Room ownership verified for owner {}", ownerId);
        return room;
    }

    /**
     * Verify meter belongs to owner
     *
     * @param meterId Meter ID
     * @param ownerId Owner ID
     * @throws ForbiddenException if meter doesn't belong to owner
     * @return Meter entity if authorized
     */
    public Meter verifyMeterOwnership(String meterId, Long ownerId) {
        log.debug("Verifying meter {} ownership by owner {}", meterId, ownerId);

        Meter meter = meterRepository.findByMeterId(meterId)
                .orElseThrow(() -> new MeterNotFoundException(meterId));

        if (!meter.getOwnerId().equals(ownerId)) {
            log.warn("Meter ownership denied: Owner {} does not own meter {}", ownerId, meterId);
            throw new ForbiddenException("You don't have permission to manage this meter");
        }

        log.debug("Meter ownership verified for owner {}", ownerId);
        return meter;
    }

    /**
     * Check if user is an active tenant in a specific room
     *
     * @param userId User ID
     * @param roomId Room ID
     * @return true if user has active booking in room
     */
    public boolean isActiveTenantInRoom(Long userId, Long roomId) {
        log.debug("Checking if user {} is active tenant in room {}", userId, roomId);

        Optional<BookingRequest> activeBooking = bookingRequestRepository
                .findActiveBookingByUserIdAndRoomId(userId, roomId);

        boolean isActive = activeBooking.isPresent();
        log.debug("Active tenant check for user {} in room {}: {}", userId, roomId, isActive);

        return isActive;
    }

    /**
     * Verify user has permission to view meter history
     * Same as view access (owner or tenant)
     *
     * @param meterId Meter ID
     * @param userId Current user ID
     * @param userRole Current user role
     * @return Meter entity if authorized
     */
    public Meter verifyCanViewHistory(String meterId, Long userId, Role userRole) {
        // History viewing has same permissions as live viewing
        return verifyCanViewMeter(meterId, userId, userRole);
    }

    /**
     * Verify meter exists and is paired
     *
     * @param meterId Meter ID
     * @throws MeterNotFoundException if meter not found
     * @throws IllegalStateException if meter is not paired
     * @return Meter entity
     */
    public Meter verifyMeterPaired(String meterId) {
        log.debug("Verifying meter {} is paired", meterId);

        Meter meter = meterRepository.findByMeterId(meterId)
                .orElseThrow(() -> new MeterNotFoundException(meterId));

        if (!meter.isPaired()) {
            log.warn("Meter {} is not paired", meterId);
            throw new IllegalStateException("Meter is not paired to any room");
        }

        return meter;
    }

    /**
     * Check if room already has a meter
     *
     * @param roomId Room ID
     * @return true if room has a meter
     */
    public boolean roomHasMeter(Long roomId) {
        boolean hasMeter = meterRepository.existsByRoomId(roomId);
        log.debug("Room {} has meter: {}", roomId, hasMeter);
        return hasMeter;
    }

    /**
     * Check if meter is already paired
     *
     * @param meterId Meter ID
     * @return true if meter is already paired
     */
    public boolean isMeterPaired(String meterId) {
        Optional<Meter> meterOpt = meterRepository.findByMeterId(meterId);
        if (meterOpt.isEmpty()) {
            return false;
        }
        boolean isPaired = meterOpt.get().isPaired();
        log.debug("Meter {} is paired: {}", meterId, isPaired);
        return isPaired;
    }

    /**
     * Get meter with ownership verification
     *
     * @param meterId Meter ID
     * @param ownerId Owner ID
     * @return Meter entity
     */
    public Meter getMeterWithOwnershipCheck(String meterId, Long ownerId) {
        return verifyMeterOwnership(meterId, ownerId);
    }
}
