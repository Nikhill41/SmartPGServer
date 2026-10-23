package com.pgmanagement.repository;

import com.pgmanagement.dto.response.RoomResponse;
import com.pgmanagement.entity.Room;
import com.pgmanagement.enums.RoomStatus;
import com.pgmanagement.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Room entity operations
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    /**
     * Find all rooms in a specific PG
     */
    List<Room> findByPgId(Long pgId);

    /**
     * Find available rooms in a PG
     */
    List<Room> findByPgIdAndStatus(Long pgId, RoomStatus status);

    /**
     * Find room by PG ID and room number
     */
    Optional<Room> findByPgIdAndRoomNumber(Long pgId, String roomNumber);

    /**
     * Find room with pessimistic lock (for booking operations)
     * This prevents race conditions during concurrent bookings
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :roomId")
    Optional<Room> findByIdWithLock(@Param("roomId") Long roomId);

    /**
     * Find available rooms by type in a city
     */
    @Query("SELECT r FROM Room r WHERE r.pg.city = :city AND r.type = :type AND r.status = 'AVAILABLE'")
    List<Room> findAvailableRoomsByTypeInCity(@Param("city") String city, @Param("type") RoomType type);

    /**
     * Find available rooms within rent range
     */
    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE' AND r.rent BETWEEN :minRent AND :maxRent")
    List<Room> findAvailableRoomsByRentRange(@Param("minRent") BigDecimal minRent,
                                             @Param("maxRent") BigDecimal maxRent);

    /**
     * Check if room exists and is available
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r WHERE r.id = :roomId AND r.status = 'AVAILABLE'")
    boolean isRoomAvailable(@Param("roomId") Long roomId);

    /**
     * Verify room belongs to owner
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Room r WHERE r.id = :roomId AND r.pg.owner.id = :ownerId")
    boolean isRoomOwnedBy(@Param("roomId") Long roomId, @Param("ownerId") Long ownerId);

    /**
     * Count available rooms in a PG
     */
    @Query("SELECT COUNT(r) FROM Room r WHERE r.pg.id = :pgId AND r.status = 'AVAILABLE'")
    Long countAvailableRoomsInPG(@Param("pgId") Long pgId);



}
