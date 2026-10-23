package com.pgmanagement.repository;

import com.pgmanagement.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Amenity entity operations
 */
@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    /**
     * Find amenity by name (case-insensitive)
     */
    Optional<Amenity> findByNameIgnoreCase(String name);

    /**
     * Check if amenity exists by name
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find all predefined amenities (system-provided)
     */
    List<Amenity> findByIsPredefinedTrue();

    /**
     * Find all custom amenities (owner-created)
     */
    List<Amenity> findByIsPredefinedFalse();

    /**
     * Find amenities by IDs (for bulk assignment to rooms)
     */
    List<Amenity> findByIdIn(List<Long> ids);

    /**
     * Find amenities by names (for search filters)
     */
    List<Amenity> findByNameIn(List<String> names);
}
