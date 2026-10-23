package com.pgmanagement.repository;

import com.pgmanagement.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Owner entity operations
 */
@Repository
public interface OwnerRepository extends JpaRepository<Owner, Long> {

    /**
     * Find owner by email (for login and registration checks)
     */
    Optional<Owner> findByEmail(String email);

    /**
     * Check if owner exists with given email
     */
    boolean existsByEmail(String email);

    /**
     * Find verified owner by email
     */
    Optional<Owner> findByEmailAndIsVerifiedTrue(String email);
}
