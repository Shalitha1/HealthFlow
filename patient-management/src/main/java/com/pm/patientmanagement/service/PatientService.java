package com.pm.patientmanagement.service;

import com.pm.patientmanagement.client.BillingServiceGrpcClient;
import com.pm.patientmanagement.dto.PatientRequestDTO;
import com.pm.patientmanagement.exception.PatientNotFoundException;
import com.pm.patientmanagement.exception.EmailAlreadyExistsException;
import com.pm.patientmanagement.model.Patient;
import com.pm.patientmanagement.repository.PatientRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final BillingServiceGrpcClient billingServiceGrpcClient;

    public PatientService(PatientRepository patientRepository,
                          BillingServiceGrpcClient billingServiceGrpcClient) {
        this.patientRepository = patientRepository;
        this.billingServiceGrpcClient = billingServiceGrpcClient;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));
    }

    public Patient createPatient(PatientRequestDTO requestDTO) {
        // 1. Save patient to database first
        Patient patient = new Patient();
        patient.setName(requestDTO.getName());
        patient.setEmail(requestDTO.getEmail());
        patient.setAddress(requestDTO.getAddress());
        patient.setDateOfBirth(requestDTO.getDateOfBirth());
        patient.setRegisteredDate(LocalDate.now());

        Patient savedPatient = patientRepository.save(patient);

        // 2. Call gRPC to create billing account
        try {
            String billingAccountId = billingServiceGrpcClient.createBillingAccount(
                    savedPatient.getId().toString(),
                    savedPatient.getName(),
                    savedPatient.getEmail()
            );
            System.out.println("Successfully created billing account: " + billingAccountId);
        } catch (Exception e) {
            System.err.println("Warning: Failed to create billing account, but patient was saved: " + e.getMessage());
            // In a real system, you might want to store this in a retry queue or send to Kafka
        }

        return savedPatient;
    }

    public Patient updatePatient(UUID id, PatientRequestDTO requestDTO) {
        Patient patient = getPatientById(id);

        if (!patient.getEmail().equals(requestDTO.getEmail())
                && patientRepository.existsByEmail(requestDTO.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists: " + requestDTO.getEmail());
        }

        patient.setName(requestDTO.getName());
        patient.setEmail(requestDTO.getEmail());
        patient.setAddress(requestDTO.getAddress());
        patient.setDateOfBirth(requestDTO.getDateOfBirth());

        return patientRepository.save(patient);
    }

    public void deletePatient(UUID id) {
        getPatientById(id);
        patientRepository.deleteById(id);
    }
}