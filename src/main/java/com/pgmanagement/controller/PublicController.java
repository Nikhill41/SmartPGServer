package com.pgmanagement.controller;

import com.pgmanagement.dto.request.BookingRequestDTO;
import com.pgmanagement.dto.request.PGSearchRequest;
import com.pgmanagement.dto.response.*;
import com.pgmanagement.entity.Room;
import com.pgmanagement.repository.RoomRepository;
import com.pgmanagement.security.UserDetailsImpl;
import com.pgmanagement.service.BookingService;
import com.pgmanagement.service.SearchService;
import com.pgmanagement.service.UserService;
import com.pgmanagement.util.EntityMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Public Controller
 * Handles: Search PGs, View Details, Create Booking (authenticated)
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class PublicController {

    private final SearchService searchService;
    private final UserService userService;
    private final BookingService bookingService;
    private final RoomRepository roomRepository;  // ✅ ADDED
    private final EntityMapper entityMapper;

    // ==================== SEARCH & BROWSE ====================

    /**
     * Search PGs with comprehensive filters
     */
    @GetMapping("/pg/search")
    public ResponseEntity<ApiResponse<List<PGResponse>>> searchPGs(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String genderPreference,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent,
            @RequestParam(required = false) List<String> amenities) {

        log.info("Searching PGs with filters - City: {}, Gender: {}, RoomType: {}",
                city, genderPreference, roomType);

        PGSearchRequest searchRequest = new PGSearchRequest();
        searchRequest.setCity(city);
        searchRequest.setGenderPreference(
                genderPreference != null ?
                        com.pgmanagement.enums.GenderPreference.valueOf(genderPreference) : null
        );
        searchRequest.setRoomType(
                roomType != null ?
                        com.pgmanagement.enums.RoomType.valueOf(roomType) : null
        );
        searchRequest.setMinRent(minRent);
        searchRequest.setMaxRent(maxRent);
        searchRequest.setAmenities(amenities);

        List<PGResponse> results = searchService.searchPGs(searchRequest);

        return ResponseEntity.ok(
                ApiResponse.success("Search results retrieved successfully", results)
        );
    }

    /**
     * Get PG details (public view)
     */
    @GetMapping("/pg/{pgId}")
    public ResponseEntity<ApiResponse<PGResponse>> getPGDetails(@PathVariable Long pgId) {
        log.info("Fetching PG details: {}", pgId);
        PGResponse pgResponse = userService.getPGDetails(pgId);

        return ResponseEntity.ok(
                ApiResponse.success("PG details retrieved successfully", pgResponse)
        );
    }

    /**
     * Get room details (public view)
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomDetails(@PathVariable Long roomId) {
        log.info("Fetching room details: {}", roomId);
        RoomResponse roomResponse = userService.getRoomDetails(roomId);

        return ResponseEntity.ok(
                ApiResponse.success("Room details retrieved successfully", roomResponse)
        );
    }

    /**
     * Search available rooms
     */
    @GetMapping("/rooms/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> searchAvailableRooms(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minRent,
            @RequestParam(required = false) BigDecimal maxRent) {

        log.info("Searching available rooms - City: {}, Rent: {} - {}", city, minRent, maxRent);
        List<RoomResponse> rooms = searchService.searchAvailableRooms(city, minRent, maxRent);

        return ResponseEntity.ok(
                ApiResponse.success("Available rooms retrieved successfully", rooms)
        );
    }


    @GetMapping("/pg/{pgId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByPG(@PathVariable Long pgId) {
        log.info("Fetching all rooms for PG: {}", pgId);

        List<Room> rooms = roomRepository.findByPgId(pgId);

        List<RoomResponse> roomResponses = rooms.stream()
                .map(entityMapper::toRoomResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiResponse.success("Rooms retrieved successfully", roomResponses)
        );
    }





    // ==================== BOOKING ====================

    /**
     * Create booking request (AUTHENTICATED)
     */
//    @PostMapping("/booking/request")
//    public ResponseEntity<ApiResponse<BookingRequestResponse>> createBookingRequest(
//            @AuthenticationPrincipal UserDetailsImpl userDetails,
//            @Valid @RequestBody BookingRequestDTO request) {
//
//        log.info("Creating booking request - User: {}, Room: {}",
//                userDetails.getId(), request.getRoomId());
//
//        BookingRequestResponse booking =
//                bookingService.createBookingRequest(userDetails.getId(), request);
//
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.success(
//                        "Booking request submitted successfully. Owner will review your request.",
//                        booking
//                ));
//    }

    /**
     * Get booking details
     */
//    @GetMapping("/booking/{bookingId}")
//    public ResponseEntity<ApiResponse<BookingRequestResponse>> getBookingDetails(
//            @PathVariable Long bookingId) {
//
//        log.info("Fetching booking details: {}", bookingId);
//        BookingRequestResponse booking = bookingService.getBookingById(bookingId);
//
//        return ResponseEntity.ok(
//                ApiResponse.success("Booking details retrieved successfully", booking)
//        );
//    }
}
