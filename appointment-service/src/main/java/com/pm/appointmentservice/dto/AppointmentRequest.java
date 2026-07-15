package com.pm.appointmentservice.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record AppointmentRequest(
        @NotNull(message = "Patient ID is required") @Positive(message = "Patient ID must be positive") Long patientId,
        @NotNull(message = "Doctor ID is required") @Positive(message = "Doctor ID must be positive") Long doctorId,
        @NotNull(message = "Appointment date and time is required") @Future(message = "Appointment must be in the future") OffsetDateTime appointmentDateTime,
        @NotNull(message = "Duration is required") @Min(value = 5, message = "Duration must be at least 5 minutes") @Max(value = 480, message = "Duration must not exceed 480 minutes") Integer durationMinutes,
        @NotBlank(message = "Reason is required") @Size(max = 500, message = "Reason must not exceed 500 characters") String reason,
        @Size(max = 5000, message = "Notes must not exceed 5000 characters") String notes
) {}
