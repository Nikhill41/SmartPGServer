package com.pgmanagement.service;

import com.pgmanagement.dto.request.AdvancedPGSearchRequest;
import com.pgmanagement.dto.request.PGSearchRequest;
import com.pgmanagement.dto.response.PGResponse;
import com.pgmanagement.dto.response.RoomResponse;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.Room;
import com.pgmanagement.enums.RoomStatus;
import com.pgmanagement.repository.PGRepository;
import com.pgmanagement.repository.RoomRepository;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for public search operations (no authentication required)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final PGRepository pgRepository;
    private final RoomRepository roomRepository;
    private final EntityMapper entityMapper;
    //advance search
    private final AdvancedPGSearchService advancedSearchService;

    /**
     * Search PGs with comprehensive filters
     */
//    public List<PGResponse> searchPGs(PGSearchRequest request) {
//        log.info("Searching PGs with filters: {}", request);
//
//        List<PG> pgs;
//
//        // If amenities filter provided, use amenity-based search
//        if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
//            pgs = pgRepository.findPGsWithAmenities(
//                    request.getCity(),
//                    request.getAmenities(),
//                    request.getAmenities().size()
//            );
//        } else {
//            // Standard search with city, gender, room type, rent range
//            pgs = pgRepository.searchPGs(
//                    request.getCity(),
//                    request.getGenderPreference(),
//                    request.getRoomType() != null ? request.getRoomType().toString() : null,
//                    request.getMinRent() != null ? request.getMinRent() : BigDecimal.ZERO,
//                    request.getMaxRent() != null ? request.getMaxRent() : new BigDecimal("100000")
//            );
//        }
//
//        // Convert to response DTOs
//        return pgs.stream()
//                .map(pg -> {
//                    // Filter rooms based on search criteria
//                    List<Room> filteredRooms = pg.getRooms().stream()
//                            .filter(room -> room.getStatus() == RoomStatus.AVAILABLE)
//                            .filter(room -> {
//                                if (request.getRoomType() != null) {
//                                    return room.getType() == request.getRoomType();
//                                }
//                                return true;
//                            })
//                            .filter(room -> {
//                                BigDecimal rent = room.getRent();
//                                BigDecimal minRent = request.getMinRent() != null ? request.getMinRent() : BigDecimal.ZERO;
//                                BigDecimal maxRent = request.getMaxRent() != null ? request.getMaxRent() : new BigDecimal("100000");
//                                return rent.compareTo(minRent) >= 0 && rent.compareTo(maxRent) <= 0;
//                            })
//                            .collect(Collectors.toList());
//
//                    int totalRooms = pg.getRooms().size();
//                    int availableRooms = filteredRooms.size();
//
//                    return entityMapper.toPGResponse(pg, totalRooms, availableRooms);
//                })
//                .filter(pgResponse -> pgResponse.getAvailableRooms() > 0) // Only return PGs with available rooms
//                .collect(Collectors.toList());
//    }



    // search pg by advance search method
    public List<PGResponse> searchPGs(PGSearchRequest oldRequest) {

        AdvancedPGSearchRequest request = new AdvancedPGSearchRequest();

        request.setCity(oldRequest.getCity());
        request.setGenderPreference(oldRequest.getGenderPreference());
        request.setRoomType(oldRequest.getRoomType());
        request.setMinRent(oldRequest.getMinRent());
        request.setMaxRent(oldRequest.getMaxRent());
        request.setAmenities(oldRequest.getAmenities());

        return advancedSearchService.search(request);
    }

    /**
     * Get available rooms with filters
     */
    public List<RoomResponse> searchAvailableRooms(String city, BigDecimal minRent, BigDecimal maxRent) {
        log.info("Searching available rooms in city: {} with rent range: {} - {}", city, minRent, maxRent);

        List<Room> rooms = roomRepository.findAvailableRoomsByRentRange(
                minRent != null ? minRent : BigDecimal.ZERO,
                maxRent != null ? maxRent : new BigDecimal("100000")
        );

        // Filter by city if provided
        if (city != null && !city.isEmpty()) {
            rooms = rooms.stream()
                    .filter(room -> room.getPg().getCity().equalsIgnoreCase(city))
                    .collect(Collectors.toList());
        }

        return rooms.stream()
                .map(entityMapper::toRoomResponse)
                .collect(Collectors.toList());
    }
}
