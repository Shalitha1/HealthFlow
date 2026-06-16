package com.pm.patientmanagement.service;

import com.pm.patientmanagement.dto.PatientRequestDTO;
import com.pm.patientmanagement.exception.PatientNotFoundException;
import com.pm.patientmanagement.model.Patient;
import com.pm.patientmanagement.repository.PatientRepository;
import org.springframework.stereotype.Service;
import com.pm.patientmanagement.exception.EmailAlreadyExistsException;
import org.springframework.web.bind.annotation.PutMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PatientService {


    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Patient getPatientById(UUID id) {

        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException( "Patient not found with id: " + id));
    }

    public Patient createPatient(PatientRequestDTO requestDTO) {

        Patient patient = new Patient();

        patient.setName(requestDTO.getName());
        patient.setEmail(requestDTO.getEmail());
        patient.setAddress(requestDTO.getAddress());
        patient.setDateOfBirth(requestDTO.getDateOfBirth());

        patient.setRegisteredDate(LocalDate.now());

        return patientRepository.save(patient);
    }
    public Patient updatePatient(UUID id, PatientRequestDTO requestDTO) {

        Patient patient = getPatientById(id);

        if (!patient.getEmail().equals(requestDTO.getEmail())
                && patientRepository.existsByEmail(requestDTO.getEmail())) {

            throw new EmailAlreadyExistsException(
                    "Email already exists: " + requestDTO.getEmail()
            );
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