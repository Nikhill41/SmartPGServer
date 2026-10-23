package com.pgmanagement.repository;

import com.pgmanagement.entity.PG;
import com.pgmanagement.enums.GenderPreference;
import com.pgmanagement.enums.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for PG entity operations with advanced search capabilities
 */
@Repository
public interface PGRepository extends JpaRepository<PG, Long> {

    /**
     * Find all PGs owned by a specific owner
     */
    List<PG> findByOwnerId(Long ownerId);

    /**
     * Find PG by ID and verify ownership
     */
    @Query("SELECT p FROM PG p WHERE p.id = :pgId AND p.owner.id = :ownerId")
    Optional<PG> findByIdAndOwnerId(@Param("pgId") Long pgId, @Param("ownerId") Long ownerId);

    /**
     * Search PGs by city
     */
    List<PG> findByCity(String city);

    /**
     * Search PGs by city (case-insensitive)
     */
    List<PG> findByCityIgnoreCase(String city);

    /**
     * Search PGs by gender preference
     */
    List<PG> findByGenderPreference(GenderPreference genderPreference);

    /**
     * Advanced search: City + Gender Preference
     */
    List<PG> findByCityIgnoreCaseAndGenderPreference(String city, GenderPreference genderPreference);

    /**
     * Search PGs with available rooms in a city
     */
    @Query("SELECT DISTINCT p FROM PG p JOIN p.rooms r WHERE LOWER(p.city) = LOWER(:city) AND r.status = 'AVAILABLE'")
    List<PG> findPGsWithAvailableRoomsInCity(@Param("city") String city);

    /**
     * Complex search: City + Gender + Rent range + Room type
     */
    @Query("SELECT DISTINCT p FROM PG p " +
            "JOIN p.rooms r " +
            "WHERE LOWER(p.city) = LOWER(:city) " +
            "AND (:gender IS NULL OR p.genderPreference = :gender OR p.genderPreference = 'BOTH') " +
            "AND (:roomType IS NULL OR r.type = :roomType) " +
            "AND r.rent BETWEEN :minRent AND :maxRent " +
            "AND r.status = 'AVAILABLE'")
    List<PG> searchPGs(@Param("city") String city,
                       @Param("gender") GenderPreference gender,
                       @Param("roomType") String roomType,
                       @Param("minRent") BigDecimal minRent,
                       @Param("maxRent") BigDecimal maxRent);

    /**
     * Search PGs with specific amenities
     */
    @Query("SELECT DISTINCT p FROM PG p " +
            "JOIN p.rooms r " +
            "JOIN r.amenities a " +
            "WHERE LOWER(p.city) = LOWER(:city) " +
            "AND a.name IN :amenityNames " +
            "AND r.status = 'AVAILABLE' " +
            "GROUP BY p.id " +
            "HAVING COUNT(DISTINCT a.name) = :amenityCount")
    List<PG> findPGsWithAmenities(@Param("city") String city,
                                  @Param("amenityNames") List<String> amenityNames,
                                  @Param("amenityCount") long amenityCount);




    //advance search query

    @Query("""
SELECT p FROM PG p
JOIN p.rooms r
LEFT JOIN r.amenities a
WHERE 
    (:city IS NULL OR 
     LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%')) OR 
     LOWER(p.name) LIKE LOWER(CONCAT('%', :city, '%')) OR 
     LOWER(p.address) LIKE LOWER(CONCAT('%', :city, '%')) OR 
     LOWER(p.state) LIKE LOWER(CONCAT('%', :city, '%')))
AND (:gender IS NULL OR p.genderPreference = :gender OR p.genderPreference = 'BOTH')
AND (:roomType IS NULL OR r.type = :roomType)
AND (r.rent BETWEEN :minRent AND :maxRent)
AND r.status = 'AVAILABLE'
AND (:amenities IS NULL OR a.name IN :amenities)
""")
    Page<PG> advancedSearch(
            @Param("city") String city,
            @Param("gender") GenderPreference gender,
            @Param("roomType") RoomType roomType,
            @Param("minRent") BigDecimal minRent,
            @Param("maxRent") BigDecimal maxRent,
            @Param("amenities") List<String> amenities,
            Pageable pageable
    );


}
