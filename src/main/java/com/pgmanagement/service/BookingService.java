package com.pgmanagement.service;

import com.pgmanagement.dto.request.BookingActionRequest;
import com.pgmanagement.dto.response.BookingRequestResponse;
import com.pgmanagement.entity.BookingRequest;
import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.User;
import com.pgmanagement.enums.BookingStatus;
import com.pgmanagement.enums.GenderPreference;
import com.pgmanagement.exception.*;
import com.pgmanagement.repository.BookingRequestRepository;
import com.pgmanagement.repository.RoomRepository;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.pgmanagement.entity.TenantStayRecord;
import com.pgmanagement.enums.StayStatus;
import com.pgmanagement.repository.TenantStayRecordRepository;
import java.time.LocalDate;

/**
 * Service for complete booking workflow
 * Handles: Request → Owner Review → Accept/Reject → Tenant Assignment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final SecurityDepositService securityDepositService; // ✅ ADD THIS
    private final BookingRequestRepository bookingRequestRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final EmailService emailService;
    private final EntityMapper entityMapper;
    private final TenantStayRecordRepository tenantStayRecordRepository;

    /**
     * USER: Create booking request (authenticated)
     * ✅ FIXED: Changed signature to (Long userId, Long roomId)
     */
    @Transactional
    public BookingRequestResponse createBookingRequest(Long userId, Long roomId) {
        log.info("Creating booking request - User: {}, Room: {}", userId, roomId);

        // Find user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Find room with pessimistic lock (prevents race conditions)
        Room room = roomRepository.findByIdWithLock(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        // Validation 1: Check if room is available
        if (!room.hasAvailableSpace()) {
            throw new RoomFullException(room.getRoomNumber());
        }

        // Validation 2: Check gender compatibility
        validateGenderPreference(user, room);

        // Validation 3: Check for duplicate pending booking
        if (bookingRequestRepository.hasExistingPendingBooking(userId, roomId)) {
            throw new DuplicateBookingException();
        }

        // Create booking request
        BookingRequest bookingRequest = new BookingRequest();
        bookingRequest.setUser(user);
        bookingRequest.setRoom(room);
        bookingRequest.setStatus(BookingStatus.PENDING);
        bookingRequest.setRequestDate(LocalDateTime.now());

        bookingRequest = bookingRequestRepository.save(bookingRequest);

        log.info("Booking request created successfully. Booking ID: {}", bookingRequest.getId());

        // Send notification email to owner
        try {
            emailService.sendNewBookingNotificationToOwner(
                    room.getPg().getOwner().getEmail(),
                    room.getPg().getOwner().getName(),
                    user.getName(),
                    room.getPg().getName(),
                    room.getRoomNumber()
            );
        } catch (Exception e) {
            log.error("Failed to send booking notification email to owner", e);
            // Don't fail the booking if email fails
        }

        return entityMapper.toBookingRequestResponse(bookingRequest);
    }

    /**
     * OWNER: Get all booking requests for their PGs
     */
    public List<BookingRequestResponse> getBookingRequestsForOwner(Long ownerId, BookingStatus status) {
        log.info("Fetching booking requests for owner: {} with status: {}", ownerId, status);

        List<BookingRequest> bookings;
        if (status != null) {
            bookings = bookingRequestRepository.findBookingsByOwnerAndStatus(ownerId, status);
        } else {
            bookings = bookingRequestRepository.findBookingRequestsForOwner(ownerId);
        }

        return bookings.stream()
                .map(entityMapper::toBookingRequestResponse)
                .collect(Collectors.toList());
    }


    /**
     * OWNER: Accept booking request (TRANSACTIONAL - Critical Operation)
     * ✅ UPDATED: Now generates security deposit bill automatically
     */
    @Transactional
    public BookingRequestResponse acceptBooking(Long ownerId, Long bookingId) {
        log.info("Owner {} accepting booking request: {}", ownerId, bookingId);

        // Find booking and verify ownership
        BookingRequest booking = bookingRequestRepository.findByIdAndOwnerId(bookingId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to accept this booking"));

        // Validation 1: Check booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStatusException(
                    booking.getStatus().toString(),
                    "accept"
            );
        }

        // Lock the room (prevents concurrent booking acceptance)
        Room room = roomRepository.findByIdWithLock(booking.getRoom().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", booking.getRoom().getId()));

        // Validation 2: Verify room still has capacity
        if (!room.hasAvailableSpace()) {
            booking.setStatus(BookingStatus.REJECTED);
            booking.setRejectionReason("Room is now full");
            booking.setResponseDate(LocalDateTime.now());
            bookingRequestRepository.save(booking);
            throw new RoomFullException(room.getRoomNumber());
        }

        User user = booking.getUser();
        LocalDateTime moveInDate = LocalDateTime.now();

        // Step 1: Assign tenant to room
        user.setRoom(room);
        user.setIsActive(true);
        user.setMoveInDate(moveInDate);
        userRepository.save(user);
        log.info("User {} assigned to room {}", user.getId(), room.getId());

        // Step 2: Update room occupancy
        room.incrementOccupancy();
        roomRepository.save(room);
        log.info("Room {} occupancy updated: {}/{}", room.getId(), room.getCurrentOccupancy(), room.getCapacity());

        // Step 3: Update booking status
        booking.setStatus(BookingStatus.ACCEPTED);
        booking.setResponseDate(LocalDateTime.now());
        bookingRequestRepository.save(booking);
        log.info("Booking {} accepted successfully", bookingId);

        // ✅ NEW Step 4: Create TenantStayRecord for billing system
        try {
            TenantStayRecord stayRecord = new TenantStayRecord();
            stayRecord.setTenantId(user.getId());
            stayRecord.setRoomId(room.getId());
            stayRecord.setBookingId(bookingId);
            stayRecord.setMoveInDate(moveInDate.toLocalDate());
            stayRecord.setStayStatus(StayStatus.ACTIVE);
            stayRecord.setCheckoutInitiated(false);
            tenantStayRecordRepository.save(stayRecord);
            log.info("TenantStayRecord created for tenant {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to create TenantStayRecord for tenant {}: {}",
                    user.getId(), e.getMessage(), e);
        }
        // ✅ NEW: Step 4: Generate security deposit bill
        try {
            securityDepositService.generateSecurityDepositBill(
                    user.getId(),
                    room.getId(),
                    moveInDate.toLocalDate()
            );
            log.info("Security deposit bill generated for tenant {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to generate security deposit bill for tenant {}: {}",
                    user.getId(), e.getMessage(), e);
            // Don't rollback booking if security deposit generation fails
            // Owner can manually generate it later
        }

        // Step 5: Send confirmation email to user
        try {
            emailService.sendBookingConfirmationEmail(
                    user.getEmail(),
                    user.getName(),
                    room.getPg().getName(),
                    room.getRoomNumber(),
                    room.getRent().toString()
            );
        } catch (Exception e) {
            log.error("Failed to send booking confirmation email", e);
        }

        return entityMapper.toBookingRequestResponse(booking);
    }


    /**
     * OWNER: Reject booking request
     */
    @Transactional
    public BookingRequestResponse rejectBooking(Long ownerId, Long bookingId, BookingActionRequest request) {
        log.info("Owner {} rejecting booking request: {}", ownerId, bookingId);

        // Validate rejection reason
        if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        // Find booking and verify ownership
        BookingRequest booking = bookingRequestRepository.findByIdAndOwnerId(bookingId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to reject this booking"));

        // Validation: Check booking status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStatusException(
                    booking.getStatus().toString(),
                    "reject"
            );
        }

        // Update booking status
        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(request.getRejectionReason());
        booking.setResponseDate(LocalDateTime.now());

        bookingRequestRepository.save(booking);

        log.info("Booking {} rejected successfully", bookingId);

        // Send rejection email to user
        try {
            emailService.sendBookingRejectionEmail(
                    booking.getUser().getEmail(),
                    booking.getUser().getName(),
                    booking.getRoom().getPg().getName(),
                    booking.getRoom().getRoomNumber(),
                    request.getRejectionReason()
            );
        } catch (Exception e) {
            log.error("Failed to send booking rejection email", e);
            // Don't fail the operation if email fails
        }

        return entityMapper.toBookingRequestResponse(booking);
    }

    /**
     * USER: Get my booking requests
     */
    public List<BookingRequestResponse> getUserBookings(Long userId) {
        log.info("Fetching booking requests for user: {}", userId);

        List<BookingRequest> bookings = bookingRequestRepository.findByUserId(userId);

        return bookings.stream()
                .map(entityMapper::toBookingRequestResponse)
                .collect(Collectors.toList());
    }

    /**
     * USER: Cancel pending booking
     */
    @Transactional
    public void cancelBooking(Long userId, Long bookingId) {
        log.info("User {} canceling booking: {}", userId, bookingId);

        BookingRequest booking = bookingRequestRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Verify ownership
        if (!booking.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to cancel this booking");
        }

        // Can only cancel pending bookings
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidBookingStatusException(
                    booking.getStatus().toString(),
                    "cancel"
            );
        }

        // Delete booking
        bookingRequestRepository.delete(booking);

        log.info("Booking {} canceled successfully", bookingId);
    }

    /**
     * Get single booking details
     */
    public BookingRequestResponse getBookingById(Long bookingId) {
        log.info("Fetching booking details: {}", bookingId);

        BookingRequest booking = bookingRequestRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        return entityMapper.toBookingRequestResponse(booking);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Validate if user's gender matches PG's gender preference
     */
    private void validateGenderPreference(User user, Room room) {
        GenderPreference pgGenderPref = room.getPg().getGenderPreference();

        // If PG accepts BOTH genders, allow
        if (pgGenderPref == GenderPreference.BOTH) {
            return;
        }

        // Check if user's gender matches PG preference
        if (pgGenderPref == GenderPreference.MALE &&
                user.getGender() != com.pgmanagement.enums.Gender.MALE) {
            throw new GenderMismatchException("MALE", user.getGender().toString());
        }

        if (pgGenderPref == GenderPreference.FEMALE &&
                user.getGender() != com.pgmanagement.enums.Gender.FEMALE) {
            throw new GenderMismatchException("FEMALE", user.getGender().toString());
        }
    }
}
