package com.pm.patientmanagement.service;

import com.pm.patientmanagement.client.BillingServiceClient;
import com.pm.patientmanagement.dto.PatientRequestDTO;
import com.pm.patientmanagement.dto.PatientResponseDTO;
import com.pm.patientmanagement.exception.PatientNotFoundException;
import com.pm.patientmanagement.exception.EmailAlreadyExistsException;
import com.pm.patientmanagement.kafka.PatientEventProducer;
import com.pm.patientmanagement.model.Patient;
import com.pm.patientmanagement.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Patient service - business logic
 */
@Service
public class PatientService {

    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private BillingServiceClient billingServiceClient;

    @Autowired
    private PatientEventProducer patientEventProducer;

    /**
     * Get all patients
     */
    @Transactional(readOnly = true)
    public List<PatientResponseDTO> getAllPatients() {
        logger.info("Fetching all patients");
        return patientRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get patient by ID
     */
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id) {
        logger.info("Fetching patient: {}", id);
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));
        return toResponseDTO(patient);
    }

    /**
     * Create new patient
     * - Check for duplicate email
     * - Save to database
     * - Call billing service (HTTP)
     * - Publish Kafka event
     */
    @Transactional
    public PatientResponseDTO createPatient(PatientRequestDTO requestDTO) {
        logger.info("Creating patient with email: {}", requestDTO.getEmail());

        // 1. Check if email already exists
        if (patientRepository.existsByEmail(requestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + requestDTO.getEmail());
        }

        // 2. Create patient entity
        Patient patient = Patient.builder()
                .name(requestDTO.getName())
                .email(requestDTO.getEmail())
                .address(requestDTO.getAddress())
                .dateOfBirth(requestDTO.getDateOfBirth())
                .active(true)
                .build();

        // 3. Save to database
        Patient savedPatient = patientRepository.save(patient);
        logger.info("✅ Patient saved with ID: {}", savedPatient.getId());

        // 4. Call billing service
        try {
            billingServiceClient.createBillingAccount(
                    savedPatient.getId().toString(),
                    savedPatient.getName(),
                    savedPatient.getEmail()
            );
            logger.info("✅ Billing account created");
        } catch (Exception e) {
            logger.warn("⚠️ Billing service call failed: {}", e.getMessage());
        }

        // 5. Publish Kafka event
        try {
            patientEventProducer.publishPatientCreatedEvent(
                    savedPatient.getId(),
                    savedPatient.getName(),
                    savedPatient.getEmail(),
                    savedPatient.getAddress(),
                    savedPatient.getDateOfBirth().toString()
            );
            logger.info("✅ Patient event published");
        } catch (Exception e) {
            logger.warn("⚠️ Kafka publish failed: {}", e.getMessage());
        }

        return toResponseDTO(savedPatient);
    }

    /**
     * Update patient
     */
    @Transactional
    public PatientResponseDTO updatePatient(Long id, PatientRequestDTO requestDTO) {
        logger.info("Updating patient: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));

        // Check email uniqueness
        if (!patient.getEmail().equals(requestDTO.getEmail())
                && patientRepository.existsByEmail(requestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + requestDTO.getEmail());
        }

        patient.setName(requestDTO.getName());
        patient.setEmail(requestDTO.getEmail());
        patient.setAddress(requestDTO.getAddress());
        patient.setDateOfBirth(requestDTO.getDateOfBirth());

        Patient updated = patientRepository.save(patient);
        logger.info("✅ Patient updated: {}", id);

        return toResponseDTO(updated);
    }

    /**
     * Delete patient
     */
    @Transactional
    public void deletePatient(Long id) {
        logger.info("Deleting patient: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));

        patientRepository.deleteById(id);
        logger.info("✅ Patient deleted: {}", id);
    }

    /**
     * Convert Patient entity to response DTO
     */
    private PatientResponseDTO toResponseDTO(Patient patient) {
        return PatientResponseDTO.builder()
                .id(patient.getId())
                .name(patient.getName())
                .email(patient.getEmail())
                .address(patient.getAddress())
                .dateOfBirth(patient.getDateOfBirth())
                .createdAt(patient.getCreatedAt())
                .updatedAt(patient.getUpdatedAt())
                .active(patient.getActive())
                .build();
    }
}
