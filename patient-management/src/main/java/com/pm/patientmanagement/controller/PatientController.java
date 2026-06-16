package com.pm.patientmanagement.controller;

import com.pm.patientmanagement.dto.PatientRequestDTO;
import com.pm.patientmanagement.dto.PatientResponseDTO;
import com.pm.patientmanagement.service.PatientService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Patient REST controller
 */
@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PatientController {

    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    @Autowired
    private PatientService patientService;

    /**
     * GET /api/patients
     * Get all patients
     */
    @GetMapping
    public ResponseEntity<List<PatientResponseDTO>> getAllPatients() {
        logger.info("GET /api/patients");
        List<PatientResponseDTO> patients = patientService.getAllPatients();
        return ResponseEntity.ok(patients);
    }

    /**
     * GET /api/patients/{id}
     * Get patient by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable Long id) {
        logger.info("GET /api/patients/{}", id);
        PatientResponseDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok(patient);
    }

    /**
     * POST /api/patients
     * Create new patient
     */
    @PostMapping
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Valid @RequestBody PatientRequestDTO requestDTO) {
        logger.info("POST /api/patients - Creating patient: {}", requestDTO.getEmail());
        PatientResponseDTO patient = patientService.createPatient(requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(patient);
    }

    /**
     * PUT /api/patients/{id}
     * Update patient
     */
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequestDTO requestDTO) {
        logger.info("PUT /api/patients/{}", id);
        PatientResponseDTO patient = patientService.updatePatient(id, requestDTO);
        return ResponseEntity.ok(patient);
    }

    /**
     * DELETE /api/patients/{id}
     * Delete patient
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        logger.info("DELETE /api/patients/{}", id);
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/patients/health
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Patient Service is healthy");
    }
}