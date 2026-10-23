package com.pgmanagement.service;

import com.pgmanagement.dto.request.UpdateUserProfileRequest;
import com.pgmanagement.dto.response.PGResponse;
import com.pgmanagement.dto.response.RoomResponse;
import com.pgmanagement.dto.response.UserBasicInfoDTO;
import com.pgmanagement.dto.response.UserResponse;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.Room;
import com.pgmanagement.entity.User;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.PGRepository;
import com.pgmanagement.repository.RoomRepository;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for User operations (profile, view details)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PGRepository pgRepository;
    private final RoomRepository roomRepository;
    private final EntityMapper entityMapper;

    /**
     * Get user profile (privacy-focused - returns DTO, not entity)
     * ✅ FIXED: Returns UserBasicInfoDTO instead of User entity
     */
    @Transactional(readOnly = true)
    public UserBasicInfoDTO getUserProfile(Long userId) {
        log.info("Fetching profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Convert to DTO (privacy-safe)
        UserBasicInfoDTO dto = new UserBasicInfoDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setIsActive(user.getIsActive());
        dto.setHasActiveBooking(user.getRoom() != null);

        log.info("Profile retrieved for user: {}", userId);
        return dto;
    }

    /**
     * Get PG details (public view)
     */
    public PGResponse getPGDetails(Long pgId) {
        log.info("Fetching PG details: {}", pgId);

        PG pg = pgRepository.findById(pgId)
                .orElseThrow(() -> new ResourceNotFoundException("PG", "id", pgId));

        int totalRooms = pg.getRooms().size();
        Long availableRooms = roomRepository.countAvailableRoomsInPG(pgId);

        return entityMapper.toPGResponse(pg, totalRooms, availableRooms.intValue());
    }

    /**
     * Get room details (public view)
     */
    public RoomResponse getRoomDetails(Long roomId) {
        log.info("Fetching room details: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        return entityMapper.toRoomResponse(room);
    }

    /**
     * Check if user has active booking
     */
    @Transactional(readOnly = true)
    public boolean hasActiveBooking(Long userId) {
        log.info("Checking active booking for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return user.getRoom() != null;
    }

    /**
     * Get user entity by ID (for internal service use only)
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }


    public UserResponse updateUserProfile(Long userId, UpdateUserProfileRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());

        userRepository.save(user);

        return UserResponse.fromEntity(user);
    }





}
