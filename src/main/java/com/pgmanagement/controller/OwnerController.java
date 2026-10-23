package com.pgmanagement.controller;

import com.pgmanagement.dto.request.*;
import com.pgmanagement.dto.response.*;
import com.pgmanagement.entity.Amenity;
import com.pgmanagement.enums.BookingStatus;
import com.pgmanagement.repository.AmenityRepository;
import com.pgmanagement.security.UserDetailsImpl;
import com.pgmanagement.service.BookingService;
import com.pgmanagement.service.OwnerService;
import com.pgmanagement.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Owner Controller
 * Handles: PG/Room CRUD, Booking Request Management
 */
@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
@Slf4j
public class OwnerController {

    private final OwnerService ownerService;
    private final RoomService roomService;
    private final BookingService bookingService;
    private final AmenityRepository amenityRepository;

    // ==================== PG MANAGEMENT ====================

    /**
     * Create new PG
     */
    @PostMapping("/pg")
    public ResponseEntity<ApiResponse<PGResponse>> createPG(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreatePGRequest request) {

        log.info("Creating PG for owner: {}", userDetails.getId());
        PGResponse pgResponse = ownerService.createPG(userDetails.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("PG created successfully", pgResponse));
    }

    /**
     * Update PG
     */
    @PutMapping("/pg/{pgId}")
    public ResponseEntity<ApiResponse<PGResponse>> updatePG(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long pgId,
            @Valid @RequestBody UpdatePGRequest request) {

        log.info("Updating PG {} for owner: {}", pgId, userDetails.getId());
        PGResponse pgResponse = ownerService.updatePG(userDetails.getId(), pgId, request);

        return ResponseEntity.ok(
                ApiResponse.success("PG updated successfully", pgResponse)
        );
    }

    /**
     * Delete PG
     */
    @DeleteMapping("/pg/{pgId}")
    public ResponseEntity<ApiResponse<Void>> deletePG(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long pgId) {

        log.info("Deleting PG {} for owner: {}", pgId, userDetails.getId());
        ownerService.deletePG(userDetails.getId(), pgId);

        return ResponseEntity.ok(
                ApiResponse.success("PG deleted successfully")
        );
    }

    /**
     * Get all PGs owned by this owner
     */
    @GetMapping("/pgs")
    public ResponseEntity<ApiResponse<List<PGResponse>>> getMyPGs(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Fetching all PGs for owner: {}", userDetails.getId());
        List<PGResponse> pgs = ownerService.getOwnerPGs(userDetails.getId());

        return ResponseEntity.ok(
                ApiResponse.success("PGs retrieved successfully", pgs)
        );
    }

    /**
     * Get single PG details
     */
    @GetMapping("/pg/{pgId}")
    public ResponseEntity<ApiResponse<PGResponse>> getPGById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long pgId) {

        log.info("Fetching PG {} for owner: {}", pgId, userDetails.getId());
        PGResponse pgResponse = ownerService.getPGById(userDetails.getId(), pgId);

        return ResponseEntity.ok(
                ApiResponse.success("PG retrieved successfully", pgResponse)
        );
    }

    // ==================== ROOM MANAGEMENT ====================

    /**
     * Create new room
     */
    @PostMapping("/room")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateRoomRequest request) {

        log.info("Creating room for owner: {}", userDetails.getId());
        RoomResponse roomResponse = roomService.createRoom(userDetails.getId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", roomResponse));
    }

    /**
     * Update room
     */
    @PutMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request) {

        log.info("Updating room {} for owner: {}", roomId, userDetails.getId());
        RoomResponse roomResponse = roomService.updateRoom(userDetails.getId(), roomId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Room updated successfully", roomResponse)
        );
    }

    /**
     * Delete room
     */
    @DeleteMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long roomId) {

        log.info("Deleting room {} for owner: {}", roomId, userDetails.getId());
        roomService.deleteRoom(userDetails.getId(), roomId);

        return ResponseEntity.ok(
                ApiResponse.success("Room deleted successfully")
        );
    }

    /**
     * Get all rooms in a PG
     */
    @GetMapping("/pg/{pgId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByPG(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long pgId) {

        log.info("Fetching rooms for PG {} by owner: {}", pgId, userDetails.getId());
        List<RoomResponse> rooms = roomService.getRoomsByPG(userDetails.getId(), pgId);

        return ResponseEntity.ok(
                ApiResponse.success("Rooms retrieved successfully", rooms)
        );
    }

    /**
     * Get single room details
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long roomId) {

        log.info("Fetching room {} for owner: {}", roomId, userDetails.getId());
        RoomResponse roomResponse = roomService.getRoomById(userDetails.getId(), roomId);

        return ResponseEntity.ok(
                ApiResponse.success("Room retrieved successfully", roomResponse)
        );
    }

    // ==================== BOOKING MANAGEMENT ====================

    /**
     * Get all booking requests (with optional status filter)
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingRequestResponse>>> getBookingRequests(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(required = false) BookingStatus status) {

        log.info("Fetching booking requests for owner: {} with status: {}",
                userDetails.getId(), status);

        List<BookingRequestResponse> bookings =
                bookingService.getBookingRequestsForOwner(userDetails.getId(), status);

        return ResponseEntity.ok(
                ApiResponse.success("Booking requests retrieved successfully", bookings)
        );
    }

    /**
     * Accept booking request
     */
    @PostMapping("/booking/{bookingId}/accept")
    public ResponseEntity<ApiResponse<BookingRequestResponse>> acceptBooking(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long bookingId) {

        log.info("Owner {} accepting booking: {}", userDetails.getId(), bookingId);
        BookingRequestResponse booking =
                bookingService.acceptBooking(userDetails.getId(), bookingId);

        return ResponseEntity.ok(
                ApiResponse.success("Booking accepted successfully. Tenant has been assigned to the room.", booking)
        );
    }

    /**
     * Reject booking request
     */
    @PostMapping("/booking/{bookingId}/reject")
    public ResponseEntity<ApiResponse<BookingRequestResponse>> rejectBooking(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long bookingId,
            @Valid @RequestBody BookingActionRequest request) {

        log.info("Owner {} rejecting booking: {}", userDetails.getId(), bookingId);
        BookingRequestResponse booking =
                bookingService.rejectBooking(userDetails.getId(), bookingId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Booking rejected", booking)
        );
    }


    @GetMapping("/room/{roomId}/tenants")
    public ResponseEntity<ApiResponse<List<TenantInfoDTO>>> getRoomTenants(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long roomId) {

        log.info("Owner {} fetching tenants for room: {}", userDetails.getId(), roomId);
        List<TenantInfoDTO> tenants = ownerService.getRoomTenants(userDetails.getId(), roomId);

        return ResponseEntity.ok(
                ApiResponse.success("Tenants retrieved successfully", tenants)
        );
    }

    /**
     * Remove tenant from room
     */
    @PostMapping("/room/{roomId}/remove-tenant")
    public ResponseEntity<ApiResponse<TenantRemovalResponse>> removeTenant(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody RemoveTenantRequest request) {

        log.info("Owner {} removing tenant {} from room: {}",
                userDetails.getId(), request.getUserId(), roomId);

        TenantRemovalResponse response = ownerService.removeTenant(
                userDetails.getId(),
                roomId,
                request.getUserId(),
                request.getRemovalReason()
        );

        return ResponseEntity.ok(
                ApiResponse.success("Tenant removed successfully. Room is now available for new bookings.", response)
        );
    }

    /**
     * Get all tenants across all rooms in a PG
     */
    @GetMapping("/pg/{pgId}/all-tenants")
    public ResponseEntity<ApiResponse<List<TenantInfoDTO>>> getAllTenantsInPG(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long pgId) {

        log.info("Owner {} fetching all tenants for PG: {}", userDetails.getId(), pgId);
        List<TenantInfoDTO> tenants = ownerService.getAllTenantsInPG(userDetails.getId(), pgId);

        return ResponseEntity.ok(
                ApiResponse.success("All tenants retrieved successfully", tenants)
        );
    }


    // ==================== AMENITIES ====================

    /**
     * Get all amenities (for room creation/editing)
     */
    @GetMapping("/amenities")
    public ResponseEntity<ApiResponse<List<AmenityDTO>>> getAllAmenities(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Owner {} fetching all amenities", userDetails.getId());

        List<Amenity> amenities = amenityRepository.findAll();

        List<AmenityDTO> amenityDTOs = amenities.stream()
                .map(amenity -> new AmenityDTO(
                        amenity.getId(),
                        amenity.getName(),
                        amenity.getIcon(),
                        amenity.getIsPredefined()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Amenities retrieved successfully", amenityDTOs)
        );
    }
}