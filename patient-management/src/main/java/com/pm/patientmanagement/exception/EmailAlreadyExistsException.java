package com.pm.patientmanagement.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {

    public EmailAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, "PATIENT_EMAIL_EXISTS", message);
    }
}
