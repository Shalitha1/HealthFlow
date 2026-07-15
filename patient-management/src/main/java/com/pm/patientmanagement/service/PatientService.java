package com.pm.patientmanagement.service;

import com.pm.patientmanagement.client.BillingServiceClient;
import com.pm.patientmanagement.dto.PatientPageResponseDTO;
import com.pm.patientmanagement.dto.PatientRequestDTO;
import com.pm.patientmanagement.dto.PatientResponseDTO;
import com.pm.patientmanagement.dto.PatientStatisticsDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;

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
    public PatientPageResponseDTO getPatients(
            String search,
            Boolean active,
            int page,
            int size
    ) {
        String normalizedSearch = normalizeSearch(search);
        Long patientId = parsePatientId(normalizedSearch);
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        logger.info(
                "Fetching patients: search={}, active={}, page={}, size={}",
                normalizedSearch,
                active,
                page,
                size
        );
        Page<Patient> result = patientRepository.findAll(
                buildSpecification(normalizedSearch, patientId, active),
                pageable
        );

        return new PatientPageResponseDTO(
                result.getContent().stream().map(this::toResponseDTO).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
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
        String email = normalizeEmail(requestDTO.getEmail());
        if (patientRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("Email already exists: " + requestDTO.getEmail());
        }

        // 2. Create patient entity
        Patient patient = Patient.builder()
                .name(requestDTO.getName().trim())
                .email(email)
                .address(requestDTO.getAddress().trim())
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
        String email = normalizeEmail(requestDTO.getEmail());
        if (patientRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new EmailAlreadyExistsException("Email already exists: " + requestDTO.getEmail());
        }

        patient.setName(requestDTO.getName().trim());
        patient.setEmail(email);
        patient.setAddress(requestDTO.getAddress().trim());
        patient.setDateOfBirth(requestDTO.getDateOfBirth());

        Patient updated = patientRepository.save(patient);
        logger.info("✅ Patient updated: {}", id);

        return toResponseDTO(updated);
    }

    /**
     * Soft-delete patient
     */
    @Transactional
    public void deletePatient(Long id) {
        logger.info("Soft-deleting patient: {}", id);

        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));

        patient.setActive(false);
        patientRepository.save(patient);
        logger.info("✅ Patient marked inactive: {}", id);
    }

    @Transactional
    public PatientResponseDTO updatePatientStatus(Long id, boolean active) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));

        patient.setActive(active);
        return toResponseDTO(patientRepository.save(patient));
    }

    @Transactional(readOnly = true)
    public PatientStatisticsDTO getStatistics() {
        LocalDate firstDay = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStart = firstDay.atStartOfDay();
        LocalDateTime nextMonthStart = firstDay.plusMonths(1).atStartOfDay();

        long total = patientRepository.count();
        long active = patientRepository.countByActive(true);
        long inactive = patientRepository.countByActive(false);
        long registrationsThisMonth = patientRepository
                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        monthStart,
                        nextMonthStart
                );

        return new PatientStatisticsDTO(total, active, inactive, registrationsThisMonth);
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

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search.trim();
    }

    private Long parsePatientId(String search) {
        if (search == null) {
            return null;
        }
        try {
            return Long.valueOf(search);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Specification<Patient> buildSpecification(
            String search,
            Long patientId,
            Boolean active
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (active != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), active));
            }

            if (search != null) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                List<Predicate> searchPredicates = new ArrayList<>();
                searchPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        pattern
                ));
                searchPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        pattern
                ));
                if (patientId != null) {
                    searchPredicates.add(criteriaBuilder.equal(root.get("id"), patientId));
                }
                predicates.add(criteriaBuilder.or(searchPredicates.toArray(Predicate[]::new)));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
