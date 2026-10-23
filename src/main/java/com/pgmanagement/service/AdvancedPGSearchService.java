package com.pgmanagement.service;

import com.pgmanagement.dto.request.AdvancedPGSearchRequest;
import com.pgmanagement.dto.response.PGResponse;
import com.pgmanagement.entity.PG;
import com.pgmanagement.enums.RoomStatus;
import com.pgmanagement.repository.PGRepository;
import com.pgmanagement.util.EntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedPGSearchService {

    private final PGRepository pgRepository;
    private final EntityMapper entityMapper;

    @Cacheable(
            value = "pg_search",
            key = """
            T(java.lang.String).format(
              'city=%s|gender=%s|room=%s|min=%s|max=%s|amenities=%s|page=%d|size=%d|sort=%s|dir=%s',
              #request.city,
              #request.genderPreference,
              #request.roomType,
              #request.minRent,
              #request.maxRent,
              #request.amenities,
              #request.page,
              #request.size,
              #request.sortBy,
              #request.sortDirection
            )
            """
    )
    public List<PGResponse> search(AdvancedPGSearchRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                buildSort(request)
        );

        Page<PG> page = pgRepository.advancedSearch(
                request.getCity(),
                request.getGenderPreference(),
                request.getRoomType(),
                request.getMinRent() != null ? request.getMinRent() : BigDecimal.ZERO,
                request.getMaxRent() != null ? request.getMaxRent() : new BigDecimal("100000"),
                request.getAmenities(),
                pageable
        );

        return page.getContent()
                .stream()
                .map(pg -> {
                    int totalRooms = pg.getRooms().size();
                    int availableRooms = (int) pg.getRooms().stream()
                            .filter(r -> r.getStatus() == RoomStatus.AVAILABLE)
                            .count();

                    return entityMapper.toPGResponse(pg, totalRooms, availableRooms);
                })
                .filter(res -> res.getAvailableRooms() > 0)
                .toList();
    }

    private Sort buildSort(AdvancedPGSearchRequest request) {
        //fixing as we have entitled price to room so search causng conflict
//        Sort.Direction direction =
//                "DESC".equalsIgnoreCase(request.getSortDirection())
//                        ? Sort.Direction.DESC
//                        : Sort.Direction.ASC;
//
//        return Sort.by(direction, request.getSortBy());
        return Sort.unsorted();
    }
}
