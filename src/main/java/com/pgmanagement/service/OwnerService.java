package com.pgmanagement.service;

import com.pgmanagement.dto.request.CreatePGRequest;
import com.pgmanagement.dto.request.UpdatePGRequest;
import com.pgmanagement.dto.response.PGResponse;
import com.pgmanagement.dto.response.TenantInfoDTO;
import com.pgmanagement.dto.response.TenantRemovalResponse;
import com.pgmanagement.entity.*;
import com.pgmanagement.enums.BookingStatus;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.ImageLimitExceededException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.exception.UnauthorizedException;
import com.pgmanagement.repository.*;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;



import com.pgmanagement.entity.TenantStayRecord;
import com.pgmanagement.enums.StayStatus;
import com.pgmanagement.repository.TenantStayRecordRepository;


/**
 * Service for Owner operations (PG management)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final PGRepository pgRepository;
    private final RoomRepository roomRepository;
    private final EntityMapper entityMapper;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final BookingRequestRepository bookingRequestRepository;
    private final TenantStayRecordRepository tenantStayRecordRepository;

    /**
     * Create new PG
     */
    @Transactional
    public PGResponse createPG(Long ownerId, CreatePGRequest request) {
        log.info("Creating PG for owner: {}", ownerId);

        // Validate image limit
        if (request.getImageUrls() != null && request.getImageUrls().size() > 5) {
            throw new ImageLimitExceededException("PG", 5);
        }

        // Find owner
        Owner owner = ownerRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", ownerId));

        // Create PG
        PG pg = new PG();
        pg.setOwner(owner);
        pg.setName(request.getName());
        pg.setAddress(request.getAddress());
        pg.setCity(request.getCity());
        pg.setState(request.getState());
        pg.setPincode(request.getPincode());
        pg.setDescription(request.getDescription());
        pg.setGenderPreference(request.getGenderPreference());
        pg.setImageUrls(request.getImageUrls());

        pg = pgRepository.save(pg);

        log.info("PG created successfully with id: {}", pg.getId());

        return entityMapper.toPGResponse(pg, 0, 0);
    }

    /**
     * Update existing PG
     */
    @Transactional
    public PGResponse updatePG(Long ownerId, Long pgId, UpdatePGRequest request) {
        log.info("Updating PG {} for owner: {}", pgId, ownerId);

        // Find PG and verify ownership
        PG pg = pgRepository.findByIdAndOwnerId(pgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to update this PG"));

        // Update fields (only if provided)
        if (request.getName() != null) {
            pg.setName(request.getName());
        }
        if (request.getAddress() != null) {
            pg.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            pg.setCity(request.getCity());
        }
        if (request.getState() != null) {
            pg.setState(request.getState());
        }
        if (request.getPincode() != null) {
            pg.setPincode(request.getPincode());
        }
        if (request.getDescription() != null) {
            pg.setDescription(request.getDescription());
        }
        if (request.getGenderPreference() != null) {
            pg.setGenderPreference(request.getGenderPreference());
        }
        if (request.getImageUrls() != null) {
            if (request.getImageUrls().size() > 5) {
                throw new ImageLimitExceededException("PG", 5);
            }
            pg.setImageUrls(request.getImageUrls());
        }

        pg = pgRepository.save(pg);

        log.info("PG updated successfully: {}", pgId);

        // Get room counts
        int totalRooms = roomRepository.findByPgId(pgId).size();
        Long availableRooms = roomRepository.countAvailableRoomsInPG(pgId);

        return entityMapper.toPGResponse(pg, totalRooms, availableRooms.intValue());
    }

    /**
     * Delete PG
     */
    @Transactional
    public void deletePG(Long ownerId, Long pgId) {
        log.info("Deleting PG {} for owner: {}", pgId, ownerId);

        // Find PG and verify ownership
        PG pg = pgRepository.findByIdAndOwnerId(pgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to delete this PG"));

        pgRepository.delete(pg);

        log.info("PG deleted successfully: {}", pgId);
    }

    /**
     * Get all PGs owned by owner
     */
    public List<PGResponse> getOwnerPGs(Long ownerId) {
        log.info("Fetching all PGs for owner: {}", ownerId);

        List<PG> pgs = pgRepository.findByOwnerId(ownerId);

        return pgs.stream()
                .map(pg -> {
                    int totalRooms = pg.getRooms().size();
                    Long availableRooms = roomRepository.countAvailableRoomsInPG(pg.getId());
                    return entityMapper.toPGResponse(pg, totalRooms, availableRooms.intValue());
                })
                .collect(Collectors.toList());
    }

    /**
     * Get single PG details
     */
    public PGResponse getPGById(Long ownerId, Long pgId) {
        log.info("Fetching PG {} for owner: {}", pgId, ownerId);

        PG pg = pgRepository.findByIdAndOwnerId(pgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to view this PG"));

        int totalRooms = pg.getRooms().size();
        Long availableRooms = roomRepository.countAvailableRoomsInPG(pgId);

        return entityMapper.toPGResponse(pg, totalRooms, availableRooms.intValue());
    }


    /**
     * Get current tenants in a specific room
     */
    @Transactional(readOnly = true)
    public List<TenantInfoDTO> getRoomTenants(Long ownerId, Long roomId) {
        log.info("Fetching tenants for room: {}", roomId);

        // Verify ownership
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        if (!room.getPg().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You don't have permission to view this room's tenants");
        }

        // Find all users in this room
        List<User> tenants = userRepository.findByRoom(room);

        return tenants.stream()
                .map(this::convertToTenantInfoDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all tenants across all rooms in a PG
     */
    @Transactional(readOnly = true)
    public List<TenantInfoDTO> getAllTenantsInPG(Long ownerId, Long pgId) {
        log.info("Fetching all tenants for PG: {}", pgId);

        // Verify ownership
        PG pg = pgRepository.findByIdAndOwnerId(pgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to view this PG's tenants"));

        // Find all users in this PG
        List<User> tenants = userRepository.findAllTenantsInPG(pgId);

        return tenants.stream()
                .map(this::convertToTenantInfoDTO)
                .collect(Collectors.toList());
    }

    /**
     * Remove tenant from room
     * ✅ UPDATED: Now marks accepted bookings as COMPLETED
     */
    @Transactional
    public TenantRemovalResponse removeTenant(Long ownerId, Long roomId, Long userId, String removalReason) {
        log.info("Removing tenant {} from room {}", userId, roomId);

        // Step 1: Verify ownership
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        if (!room.getPg().getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("You don't have permission to remove tenants from this room");
        }

        // Step 2: Find tenant
        User tenant = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Step 3: Verify tenant is actually in this room
        if (tenant.getRoom() == null || !tenant.getRoom().getId().equals(roomId)) {
            throw new IllegalStateException("This user is not currently in this room");
        }

        // Step 4: Calculate stay duration
        String stayDuration = calculateStayDuration(tenant.getMoveInDate());

        // Step 5: Store info for response
        Integer previousOccupancy = room.getCurrentOccupancy();
        String tenantName = tenant.getName();
        String tenantEmail = tenant.getEmail();

        // Step 6: Remove tenant from room
        tenant.setRoom(null);
        tenant.setMoveInDate(null);
        userRepository.save(tenant);

        log.info("Tenant {} removed from room {}", userId, roomId);

        // Step 7: Update room occupancy
        room.decrementOccupancy();
        roomRepository.save(room);

        log.info("Room {} occupancy updated: {}/{}", roomId, room.getCurrentOccupancy(), room.getCapacity());

        // ✅ NEW Step 7b: Mark TenantStayRecord as COMPLETED
        try {
            tenantStayRecordRepository
                    .findByTenantIdAndStayStatus(userId, StayStatus.ACTIVE)
                    .ifPresent(stay -> {
                        stay.setStayStatus(StayStatus.COMPLETED);
                        stay.setMoveOutDate(LocalDate.now());
                        stay.setDaysStayed((int) java.time.temporal.ChronoUnit.DAYS
                                .between(stay.getMoveInDate(), LocalDate.now()));
                        stay.setCheckoutCompletedAt(LocalDateTime.now());
                        stay.setUpdatedAt(LocalDateTime.now());
                        tenantStayRecordRepository.save(stay);
                        log.info("TenantStayRecord marked COMPLETED for force-removed tenant {}", userId);
                    });
        } catch (Exception e) {
            log.error("Failed to update TenantStayRecord for tenant {}: {}",
                    userId, e.getMessage(), e);
        }

        // ✅ NEW Step 8: Mark active bookings for this user+room as COMPLETED
        completeActiveBookingsForTenantInRoom(tenant, room);


        // Step 9: Reject all pending bookings for this user
        rejectAllPendingBookingsForUser(tenant);

        // Step 10: Send email notification
        try {
            emailService.sendTenantRemovalNotification(
                    tenantEmail,
                    tenantName,
                    room.getPg().getName(),
                    room.getRoomNumber(),
                    removalReason != null ? removalReason : "No reason provided",
                    stayDuration
            );
        } catch (Exception e) {
            log.error("Failed to send tenant removal email", e);
            // Don't fail the operation if email fails
        }

        // Step 11: Build response
        TenantRemovalResponse response = new TenantRemovalResponse();
        response.setRoomId(room.getId());
        response.setRoomNumber(room.getRoomNumber());
        response.setPreviousOccupancy(previousOccupancy);
        response.setCurrentOccupancy(room.getCurrentOccupancy());
        response.setStatus(room.getStatus());

        TenantRemovalResponse.RemovedTenantInfo tenantInfo = new TenantRemovalResponse.RemovedTenantInfo();
        tenantInfo.setUserId(userId);
        tenantInfo.setName(tenantName);
        tenantInfo.setStayDuration(stayDuration);
        response.setRemovedTenant(tenantInfo);

        log.info("Tenant removal completed successfully");

        return response;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Convert User entity to TenantInfoDTO
     * ✅ UPDATED: Now includes room information
     */
    private TenantInfoDTO convertToTenantInfoDTO(User user) {
        TenantInfoDTO dto = new TenantInfoDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setMoveInDate(user.getMoveInDate());

        // Calculate days stayed
        if (user.getMoveInDate() != null) {
            long daysStayed = ChronoUnit.DAYS.between(user.getMoveInDate(), LocalDateTime.now());
            dto.setDaysStayed(daysStayed);
        }

        // ✅ NEW: Include room information if user is assigned to a room
        if (user.getRoom() != null) {
            dto.setRoomId(user.getRoom().getId());
            dto.setRoomNumber(user.getRoom().getRoomNumber());
        }

        return dto;
    }

    /**
     * Calculate stay duration as human-readable string
     */
    private String calculateStayDuration(LocalDateTime moveInDate) {
        if (moveInDate == null) {
            return "Unknown";
        }

        long days = ChronoUnit.DAYS.between(moveInDate, LocalDateTime.now());

        if (days == 0) {
            return "Less than a day";
        } else if (days == 1) {
            return "1 day";
        } else if (days < 7) {
            return days + " days";
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks + (weeks == 1 ? " week" : " weeks");
        } else if (days < 365) {
            long months = days / 30;
            return months + (months == 1 ? " month" : " months");
        } else {
            long years = days / 365;
            long remainingMonths = (days % 365) / 30;
            return years + (years == 1 ? " year" : " years") +
                    (remainingMonths > 0 ? " and " + remainingMonths + (remainingMonths == 1 ? " month" : " months") : "");
        }
    }

    /**
     * ✅ NEW: Mark all accepted bookings for this user and room as COMPLETED
     * This ensures the tenant no longer sees an "active booking" in their app
     */
    private void completeActiveBookingsForTenantInRoom(User tenant, Room room) {
        List<BookingRequest> acceptedBookings =
                bookingRequestRepository.findByUserAndRoomAndStatus(
                        tenant,
                        room,
                        BookingStatus.ACCEPTED
                );

        if (acceptedBookings.isEmpty()) {
            log.info("No accepted bookings found for user {} in room {}", tenant.getId(), room.getId());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        for (BookingRequest booking : acceptedBookings) {
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setResponseDate(now);
            // No rejectionReason needed - this is a normal completion
            bookingRequestRepository.save(booking);
        }

        log.info("Marked {} booking(s) as COMPLETED for user {} in room {}",
                acceptedBookings.size(), tenant.getId(), room.getId());
    }

    /**
     * Reject all pending booking requests for a user
     */
    private void rejectAllPendingBookingsForUser(User user) {
        List<BookingRequest> pendingBookings =
                bookingRequestRepository.findByUserAndStatus(user, BookingStatus.PENDING);

        for (BookingRequest booking : pendingBookings) {
            booking.setStatus(BookingStatus.REJECTED);
            booking.setRejectionReason("Tenant removed from previous accommodation");
            booking.setResponseDate(LocalDateTime.now());
            bookingRequestRepository.save(booking);
        }

        if (!pendingBookings.isEmpty()) {
            log.info("Rejected {} pending booking(s) for user {}", pendingBookings.size(), user.getId());
        }
    }
}
