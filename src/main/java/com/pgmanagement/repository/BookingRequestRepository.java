package com.pgmanagement.repository;

import com.pgmanagement.entity.BookingRequest;
import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.User;
import com.pgmanagement.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for BookingRequest entity operations
 */
@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {

    /**
     * Find all booking requests by user
     */
    List<BookingRequest> findByUserId(Long userId);

    /**
     * Find booking requests by user and status
     */
    List<BookingRequest> findByUserIdAndStatus(Long userId, BookingStatus status);

    /**
     * Find all booking requests for a specific room
     */
    List<BookingRequest> findByRoomId(Long roomId);

    /**
     * Find booking requests for rooms owned by a specific owner
     */
    @Query("SELECT br FROM BookingRequest br WHERE br.room.pg.owner.id = :ownerId")
    List<BookingRequest> findBookingRequestsForOwner(@Param("ownerId") Long ownerId);

    /**
     * Find pending booking requests for owner
     */
    @Query("SELECT br FROM BookingRequest br WHERE br.room.pg.owner.id = :ownerId AND br.status = 'PENDING'")
    List<BookingRequest> findPendingBookingsForOwner(@Param("ownerId") Long ownerId);

    /**
     * Find booking requests by owner and status
     */
    @Query("SELECT br FROM BookingRequest br WHERE br.room.pg.owner.id = :ownerId AND br.status = :status")
    List<BookingRequest> findBookingsByOwnerAndStatus(@Param("ownerId") Long ownerId,
                                                      @Param("status") BookingStatus status);

    /**
     * Find booking request by ID and verify it belongs to owner
     */
    @Query("SELECT br FROM BookingRequest br WHERE br.id = :bookingId AND br.room.pg.owner.id = :ownerId")
    Optional<BookingRequest> findByIdAndOwnerId(@Param("bookingId") Long bookingId,
                                                @Param("ownerId") Long ownerId);

    /**
     * Check if user has existing pending booking for a room
     */
    @Query("SELECT CASE WHEN COUNT(br) > 0 THEN true ELSE false END FROM BookingRequest br " +
            "WHERE br.user.id = :userId AND br.room.id = :roomId AND br.status = 'PENDING'")
    boolean hasExistingPendingBooking(@Param("userId") Long userId, @Param("roomId") Long roomId);

    /**
     * Count pending bookings for a specific room
     */
    @Query("SELECT COUNT(br) FROM BookingRequest br WHERE br.room.id = :roomId AND br.status = 'PENDING'")
    Long countPendingBookingsForRoom(@Param("roomId") Long roomId);

    /**
     * Find latest booking request for user-room combination
     */
    @Query("SELECT br FROM BookingRequest br WHERE br.user.id = :userId AND br.room.id = :roomId ORDER BY br.requestDate DESC")
    List<BookingRequest> findLatestBookingForUserAndRoom(@Param("userId") Long userId,
                                                         @Param("roomId") Long roomId);

    /**
     * Find all pending booking requests for a specific user
     */
    List<BookingRequest> findByUserAndStatus(User user, BookingStatus status);

    List<BookingRequest> findByUserAndRoomAndStatus(
            User user,
            Room room,
            BookingStatus status
    );




    /**
     * Find active booking for a user in a specific room
     * A booking is considered active if:
     * - Status is ACCEPTED
     * - User is currently assigned to that room
     */
    @Query("SELECT br FROM BookingRequest br " +
            "WHERE br.user.id = :userId " +
            "AND br.room.id = :roomId " +
            "AND br.status = 'ACCEPTED' " +
            "AND br.user.room.id = :roomId " +
            "AND br.user.isActive = true")
    Optional<BookingRequest> findActiveBookingByUserIdAndRoomId(
            @Param("userId") Long userId,
            @Param("roomId") Long roomId
    );

}
