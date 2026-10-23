package com.pgmanagement.repository;

import com.pgmanagement.dto.response.RoomResponse;
import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User/Tenant entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email (for login and registration checks)
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists with given email
     */
    boolean existsByEmail(String email);

    /**
     * Find all active tenants in a specific room
     */
    List<User> findByRoomIdAndIsActiveTrue(Long roomId);

    /**
     * Find all tenants in a specific PG
     */
    @Query("SELECT u FROM User u WHERE u.room.pg.id = :pgId AND u.isActive = true")
    List<User> findActiveTenantsInPG(@Param("pgId") Long pgId);

    /**
     * Count active tenants in a room
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.room.id = :roomId AND u.isActive = true")
    Long countActiveTenantsInRoom(@Param("roomId") Long roomId);

    /**
     * Check if user has active room assignment
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :userId AND u.room IS NOT NULL AND u.isActive = true")
    boolean hasActiveBooking(@Param("userId") Long userId);

    /**
     * Find all users currently in a specific room
     */
    List<User> findByRoom(Room room);

    /**
     * Find all users in rooms of a specific PG
     */
    @Query("SELECT u FROM User u WHERE u.room.pg.id = :pgId AND u.room IS NOT NULL")
    List<User> findAllTenantsInPG(@Param("pgId") Long pgId);

    /**
     * Count active tenants in a room
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.room.id = :roomId")
    Long countTenantsInRoom(@Param("roomId") Long roomId);



}
