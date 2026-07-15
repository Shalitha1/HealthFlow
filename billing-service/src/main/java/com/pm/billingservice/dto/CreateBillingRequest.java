package com.pm.billingservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateBillingRequest(
        @NotBlank(message = "Patient ID is required")
        String patientId,

        @NotBlank(message = "Patient name is required")
        String name,

        @NotBlank(message = "Patient email is required")
        @Email(message = "Patient email must be valid")
        String email
) {
}
