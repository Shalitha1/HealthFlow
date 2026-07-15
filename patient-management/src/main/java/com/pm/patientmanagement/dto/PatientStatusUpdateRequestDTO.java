package com.pm.patientmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Patient active-status update")
public record PatientStatusUpdateRequestDTO(
        @NotNull(message = "Active status is required")
        Boolean active
) {
}
