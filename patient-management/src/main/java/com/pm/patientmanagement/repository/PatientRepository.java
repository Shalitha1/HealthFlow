package com.pm.patientmanagement.repository;

import com.pm.patientmanagement.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Patient repository - database operations
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Check if email already exists
     */
    boolean existsByEmail(String email);

    /**
     * Find patient by email
     */
    Optional<Patient> findByEmail(String email);

    /**
     * Find all active patients
     */
    Optional<Patient> findByIdAndActiveTrue(Long id);
}