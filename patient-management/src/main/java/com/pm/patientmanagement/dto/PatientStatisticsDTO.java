package com.pm.patientmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Patient dashboard statistics")
public record PatientStatisticsDTO(
        long totalPatients,
        long activePatients,
        long inactivePatients,
        long registrationsThisMonth
) {
}
