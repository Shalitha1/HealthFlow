package com.pm.appointmentservice.dto;

import com.pm.appointmentservice.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(
        @NotNull(message = "Status is required") AppointmentStatus status
) {}
