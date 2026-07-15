package com.pm.patientmanagement.repository;

import com.pm.patientmanagement.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Patient repository - database operations
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    /**
     * Check if email already exists
     */
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Find patient by email
     */
    Optional<Patient> findByEmail(String email);

    /**
     * Find all active patients
     */
    Optional<Patient> findByIdAndActiveTrue(Long id);

    long countByActive(boolean active);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start,
            LocalDateTime end
    );
}
