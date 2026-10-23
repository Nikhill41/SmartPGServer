package com.pgmanagement.controller;

import com.pgmanagement.dto.request.CreateBookingRequest;
import com.pgmanagement.dto.request.UpdateUserProfileRequest;
import com.pgmanagement.dto.response.ApiResponse;
import com.pgmanagement.dto.response.BookingRequestResponse;
import com.pgmanagement.dto.response.UserBasicInfoDTO;
import com.pgmanagement.dto.response.UserResponse;
import com.pgmanagement.security.UserDetailsImpl;
import com.pgmanagement.service.BookingService;
import com.pgmanagement.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User/Tenant Controller
 * Privacy-focused: Does not expose sensitive location data
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final BookingService bookingService;

    /**
     * Get user profile (basic info only - no location data)
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserBasicInfoDTO>> getProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Fetching profile for user: {}", userDetails.getId());
        UserBasicInfoDTO user = userService.getUserProfile(userDetails.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Profile retrieved successfully", user)
        );
    }

    /**
     * Get my booking requests
     */
    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<BookingRequestResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Fetching bookings for user: {}", userDetails.getId());
        List<BookingRequestResponse> bookings = bookingService.getUserBookings(userDetails.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Bookings retrieved successfully", bookings)
        );
    }

    /**
     * Cancel pending booking
     */
    @DeleteMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long bookingId) {

        log.info("User {} canceling booking: {}", userDetails.getId(), bookingId);
        bookingService.cancelBooking(userDetails.getId(), bookingId);

        return ResponseEntity.ok(
                ApiResponse.success("Booking canceled successfully")
        );
    }

    /**
     * Check if I have active booking (boolean only)
     */
    @GetMapping("/has-active-booking")
    public ResponseEntity<ApiResponse<Boolean>> hasActiveBooking(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        log.info("Checking active booking for user: {}", userDetails.getId());
        boolean hasActive = userService.hasActiveBooking(userDetails.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Active booking status retrieved", hasActive)
        );
    }

    /**
     * Create booking request
     */
    @PostMapping("/booking/request")
    public ResponseEntity<ApiResponse<BookingRequestResponse>> createBookingRequest(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreateBookingRequest request) {

        log.info("User {} creating booking request for room: {}",
                userDetails.getId(), request.getRoomId());

        BookingRequestResponse response =
                bookingService.createBookingRequest(userDetails.getId(), request.getRoomId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking request submitted successfully", response));
    }


    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        UserResponse updatedUser = userService.updateUserProfile(currentUser.getId(), request);

        return ResponseEntity.ok(
                new ApiResponse<>(
                        true,
                        "Profile updated successfully",
                        updatedUser
                )
        );
    }
}
