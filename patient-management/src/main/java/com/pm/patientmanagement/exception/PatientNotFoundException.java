package com.pm.patientmanagement.exception;

import org.springframework.http.HttpStatus;

public class PatientNotFoundException extends ApiException {

    public PatientNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", message);
    }
}
