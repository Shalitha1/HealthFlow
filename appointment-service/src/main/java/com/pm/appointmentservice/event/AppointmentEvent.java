package com.pm.appointmentservice.event;

import java.time.OffsetDateTime;

public record AppointmentEvent(
        String eventType,
        Long appointmentId,
        Long patientId,
        Long doctorId,
        OffsetDateTime appointmentDateTime,
        Integer durationMinutes,
        String status,
        String actorId,
        OffsetDateTime occurredAt
) {}
