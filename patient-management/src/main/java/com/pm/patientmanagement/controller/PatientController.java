package com.pm.patientmanagement.controller;

import com.pm.patientmanagement.model.Patient;
import com.pm.patientmanagement.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pm.patientmanagement.dto.PatientRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {

        List<Patient> patients = patientService.getAllPatients();

        return ResponseEntity.ok(patients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable UUID id) {

        Patient patient = patientService.getPatientById(id);

        return ResponseEntity.ok(patient);
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(
            @Valid @RequestBody PatientRequestDTO requestDTO) {

        Patient patient = patientService.createPatient(requestDTO);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(patient);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable UUID id,
            @Valid @RequestBody PatientRequestDTO requestDTO) {

        Patient patient = patientService.updatePatient(id, requestDTO);

        return ResponseEntity.ok(patient);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable UUID id) {

        patientService.deletePatient(id);

        return ResponseEntity.noContent().build();
    }
}

