package com.pgmanagement.service;

import com.pgmanagement.dto.request.CreateRoomRequest;
import com.pgmanagement.dto.request.UpdateRoomRequest;
import com.pgmanagement.dto.response.RoomResponse;
import com.pgmanagement.entity.Amenity;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.Room;
import com.pgmanagement.enums.RoomStatus;
import com.pgmanagement.exception.ForbiddenException;
import com.pgmanagement.exception.ImageLimitExceededException;
import com.pgmanagement.exception.ResourceNotFoundException;
import com.pgmanagement.repository.AmenityRepository;
import com.pgmanagement.repository.PGRepository;
import com.pgmanagement.repository.RoomRepository;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for Room operations (CRUD by owners)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final PGRepository pgRepository;
    private final AmenityRepository amenityRepository;
    private final EntityMapper entityMapper;

    /**
     * Create new room in PG
     */
    @Transactional
    public RoomResponse createRoom(Long ownerId, CreateRoomRequest request) {
        log.info("Creating room in PG {} for owner: {}", request.getPgId(), ownerId);

        // Validate image limit
        if (request.getImageUrls() != null && request.getImageUrls().size() > 10) {
            throw new ImageLimitExceededException("Room", 10);
        }

        // Find PG and verify ownership
        PG pg = pgRepository.findByIdAndOwnerId(request.getPgId(), ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to add rooms to this PG"));

        // Check if room number already exists
        if (roomRepository.findByPgIdAndRoomNumber(request.getPgId(), request.getRoomNumber()).isPresent()) {
            throw new IllegalArgumentException("Room number already exists in this PG");
        }

        // Get amenities
        List<Amenity> amenities = amenityRepository.findByIdIn(request.getAmenityIds());

        // Create room
        Room room = new Room();
        room.setPg(pg);
        room.setRoomNumber(request.getRoomNumber());
        room.setType(request.getType());
        room.setCapacity(request.getCapacity());
        room.setCurrentOccupancy(0);
        room.setRent(request.getRent());
        room.setStatus(RoomStatus.AVAILABLE);
        room.setImageUrls(request.getImageUrls());
        room.setAmenities(amenities);

        room = roomRepository.save(room);

        log.info("Room created successfully with id: {}", room.getId());

        return entityMapper.toRoomResponse(room);
    }

    /**
     * Update existing room
     */
    @Transactional
    public RoomResponse updateRoom(Long ownerId, Long roomId, UpdateRoomRequest request) {
        log.info("Updating room {} for owner: {}", roomId, ownerId);

        // Find room and verify ownership
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        if (!room.getPg().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to update this room");
        }

        // Update fields (only if provided)
        if (request.getRoomNumber() != null) {
            room.setRoomNumber(request.getRoomNumber());
        }
        if (request.getType() != null) {
            room.setType(request.getType());
        }
        if (request.getCapacity() != null) {
            // Validate capacity against current occupancy
            if (request.getCapacity() < room.getCurrentOccupancy()) {
                throw new IllegalArgumentException("Cannot reduce capacity below current occupancy");
            }
            room.setCapacity(request.getCapacity());

            // Update status based on new capacity
            if (room.getCurrentOccupancy().equals(request.getCapacity())) {
                room.setStatus(RoomStatus.OCCUPIED);
            } else {
                room.setStatus(RoomStatus.AVAILABLE);
            }
        }
        if (request.getRent() != null) {
            room.setRent(request.getRent());
        }
        if (request.getImageUrls() != null) {
            if (request.getImageUrls().size() > 10) {
                throw new ImageLimitExceededException("Room", 10);
            }
            room.setImageUrls(request.getImageUrls());
        }
        if (request.getAmenityIds() != null) {
            List<Amenity> amenities = amenityRepository.findByIdIn(request.getAmenityIds());
            room.setAmenities(amenities);
        }

        room = roomRepository.save(room);

        log.info("Room updated successfully: {}", roomId);

        return entityMapper.toRoomResponse(room);
    }

    /**
     * Delete room
     */
    @Transactional
    public void deleteRoom(Long ownerId, Long roomId) {
        log.info("Deleting room {} for owner: {}", roomId, ownerId);

        // Find room and verify ownership
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        if (!room.getPg().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to delete this room");
        }

        // Check if room has tenants
        if (room.getCurrentOccupancy() > 0) {
            throw new IllegalStateException("Cannot delete room with active tenants");
        }

        roomRepository.delete(room);

        log.info("Room deleted successfully: {}", roomId);
    }

    /**
     * Get all rooms in a PG
     */
    public List<RoomResponse> getRoomsByPG(Long ownerId, Long pgId) {
        log.info("Fetching rooms for PG {} by owner: {}", pgId, ownerId);

        // Verify ownership
        pgRepository.findByIdAndOwnerId(pgId, ownerId)
                .orElseThrow(() -> new ForbiddenException("You don't have permission to view this PG"));

        List<Room> rooms = roomRepository.findByPgId(pgId);

        return rooms.stream()
                .map(entityMapper::toRoomResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get single room details
     */
    public RoomResponse getRoomById(Long ownerId, Long roomId) {
        log.info("Fetching room {} for owner: {}", roomId, ownerId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room", "id", roomId));

        if (!room.getPg().getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("You don't have permission to view this room");
        }

        return entityMapper.toRoomResponse(room);
    }
}
