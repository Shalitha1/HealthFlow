package com.pm.appointmentservice.dto;

import com.pm.appointmentservice.model.Appointment;
import com.pm.appointmentservice.model.AppointmentStatus;
import java.time.OffsetDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        Long doctorId,
        OffsetDateTime appointmentDateTime,
        Integer durationMinutes,
        String reason,
        AppointmentStatus status,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(), appointment.getPatientId(), appointment.getDoctorId(),
                appointment.getAppointmentDateTime(), appointment.getDurationMinutes(),
                appointment.getReason(), appointment.getStatus(), appointment.getNotes(),
                appointment.getCreatedAt(), appointment.getUpdatedAt()
        );
    }
}
