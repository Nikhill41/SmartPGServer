package com.pgmanagement.util;

import com.pgmanagement.dto.response.*;
import com.pgmanagement.entity.*;
import com.pgmanagement.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Utility class for mapping entities to DTOs
 */
@Component
@RequiredArgsConstructor
public class EntityMapper {

    private final S3Service s3Service;

    /**
     * Map User entity to UserBasicInfoDTO
     */
    public UserBasicInfoDTO toUserBasicInfoDTO(User user) {
        UserBasicInfoDTO dto = new UserBasicInfoDTO();
        dto.setUserId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setProfileVerified(true); // Email verified via OTP
        dto.setAccountCreatedDate(user.getCreatedAt());
        dto.setHasActiveBooking(user.hasActiveBooking());
        return dto;
    }

    /**
     * Map PG entity to PGResponse
     */
    public PGResponse toPGResponse(PG pg, Integer totalRooms, Integer availableRooms) {
        PGResponse response = new PGResponse();
        response.setPgId(pg.getId());
        response.setOwnerId(pg.getOwner().getId());
        response.setOwnerName(pg.getOwner().getName());
        response.setOwnerPhone(pg.getOwner().getPhone());
        response.setName(pg.getName());
        response.setAddress(pg.getAddress());
        response.setCity(pg.getCity());
        response.setState(pg.getState());
        response.setPincode(pg.getPincode());
        response.setDescription(pg.getDescription());
        response.setGenderPreference(pg.getGenderPreference());

        // ✅ UPDATED: Convert to presigned URLs
        response.setImageUrls(s3Service.generatePresignedGetUrls(pg.getImageUrls()));

        response.setTotalRooms(totalRooms);
        response.setAvailableRooms(availableRooms);
        response.setCreatedAt(pg.getCreatedAt());
        return response;
    }

    /**
     * Map Room entity to RoomResponse
     */
    public RoomResponse toRoomResponse(Room room) {
        RoomResponse response = new RoomResponse();
        response.setRoomId(room.getId());
        response.setPgId(room.getPg().getId());
        response.setPgName(room.getPg().getName());
        response.setRoomNumber(room.getRoomNumber());
        response.setType(room.getType());
        response.setCapacity(room.getCapacity());
        response.setCurrentOccupancy(room.getCurrentOccupancy());
        response.setAvailableSlots(room.getCapacity() - room.getCurrentOccupancy());
        response.setRent(room.getRent());
        response.setStatus(room.getStatus());

        // ✅ UPDATED: Convert to presigned URLs
        response.setImageUrls(s3Service.generatePresignedGetUrls(room.getImageUrls()));

        response.setAmenities(room.getAmenities().stream()
                .map(this::toAmenityDTO)
                .collect(Collectors.toList()));
        return response;
    }

    /**
     * Map Amenity entity to AmenityDTO
     */
    public AmenityDTO toAmenityDTO(Amenity amenity) {
        return new AmenityDTO(
                amenity.getId(),
                amenity.getName(),
                amenity.getIcon(),
                amenity.getIsPredefined()
        );
    }

    /**
     * Map Room entity to RoomBasicDTO
     */
    public RoomBasicDTO toRoomBasicDTO(Room room) {
        return new RoomBasicDTO(
                room.getId(),
                room.getRoomNumber(),
                room.getType(),
                room.getCapacity(),
                room.getCurrentOccupancy(),
                room.getRent(),
                room.getStatus()
        );
    }

    /**
     * Map PG entity to PGBasicDTO
     */
    public PGBasicDTO toPGBasicDTO(PG pg) {
        return new PGBasicDTO(
                pg.getId(),
                pg.getName(),
                pg.getAddress(),
                pg.getCity(),
                pg.getGenderPreference()
        );
    }

    /**
     * Map BookingRequest entity to BookingRequestResponse
     */
    public BookingRequestResponse toBookingRequestResponse(BookingRequest booking) {
        BookingRequestResponse response = new BookingRequestResponse();
        response.setBookingId(booking.getId());
        response.setStatus(booking.getStatus());
        response.setRequestDate(booking.getRequestDate());
        response.setResponseDate(booking.getResponseDate());
        response.setRejectionReason(booking.getRejectionReason());

        // Map user details
        response.setUser(toUserBasicInfoDTO(booking.getUser()));

        // Map room details
        response.setRoom(toRoomBasicDTO(booking.getRoom()));

        // Map PG details
        response.setPg(toPGBasicDTO(booking.getRoom().getPg()));

        return response;
    }

    public BillResponse toBillResponse(Bill bill) {
        if (bill == null) return null;

        BillResponse response = new BillResponse();

        response.setId(bill.getId());
        response.setBillNumber(bill.getBillNumber());

        // IDs
        response.setTenantId(bill.getTenantId());
        response.setRoomId(bill.getRoomId());
        response.setOwnerId(bill.getOwnerId());
        response.setPgId(bill.getPgId());

        // ✅ Safely get tenant name/email from lazy relation
        try {
            if (bill.getTenant() != null) {
                response.setTenantName(bill.getTenant().getName());
                response.setTenantEmail(bill.getTenant().getEmail());
            }
        } catch (Exception e) {
            // lazy load failed — leave null, not a crash
        }

        // ✅ Safely get room number from lazy relation
        try {
            if (bill.getRoom() != null) {
                response.setRoomNumber(bill.getRoom().getRoomNumber());
            }
        } catch (Exception e) {
            // lazy load failed — leave null
        }

        // ✅ Safely get PG name from lazy relation
        try {
            if (bill.getPg() != null) {
                response.setPgName(bill.getPg().getName());
            }
        } catch (Exception e) {
            // lazy load failed — leave null
        }

        // Billing Period
        response.setBillingPeriodStart(bill.getBillingPeriodStart());
        response.setBillingPeriodEnd(bill.getBillingPeriodEnd());

        // Charges
        response.setRoomRent(bill.getRoomRent());
        response.setElectricityCharge(bill.getElectricityCharge());
        response.setOtherCharges(bill.getOtherCharges());
        response.setDiscount(bill.getDiscount());
        response.setLateFee(bill.getLateFee());
        response.setAdditionalDeductions(bill.getAdditionalDeductions());

        // GST
        response.setSubtotal(bill.getSubtotal());
        response.setGstEnabled(bill.getGstEnabled());
        response.setGstPercentage(bill.getGstPercentage());
        response.setGstAmount(bill.getGstAmount());

        // Security Deposit
        response.setSecurityDepositUsed(bill.getSecurityDepositUsed());

        // Total
        response.setTotalAmount(bill.getTotalAmount());

        // Electricity
        response.setTotalRoomUnits(bill.getTotalRoomUnits());
        response.setTenantUnitShare(bill.getTenantUnitShare());
        response.setTotalRoomTenants(bill.getTotalRoomTenants());

        // Status & Type
        response.setStatus(bill.getStatus());
        response.setBillType(bill.getBillType());
        response.setGenerationType(bill.getGenerationType());

        // Dates
        response.setDueDate(bill.getDueDate());
        response.setPaidDate(bill.getPaidDate());
        response.setGeneratedDate(bill.getGeneratedDate());

        // Payment Proof
        response.setPaymentProofUrl(bill.getPaymentProofUrl());
        response.setPaymentProofUploadedAt(bill.getPaymentProofUploadedAt());
        response.setPaymentProofStatus(bill.getPaymentProofStatus());
        response.setPaymentRejectionCount(bill.getPaymentRejectionCount());
        response.setLastRejectionReason(bill.getLastRejectionReason());

        // Version & Notes
        response.setVersion(bill.getVersion());
        response.setNotes(bill.getNotes());

        // Timestamps
        response.setCreatedAt(bill.getCreatedAt());
        response.setUpdatedAt(bill.getUpdatedAt());

        return response;
    }

}
