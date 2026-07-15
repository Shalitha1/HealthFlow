package com.pm.appointmentservice.service;

import com.pm.appointmentservice.dto.*;
import com.pm.appointmentservice.event.AppointmentEventProducer;
import com.pm.appointmentservice.exception.ApiException;
import com.pm.appointmentservice.model.*;
import com.pm.appointmentservice.repository.AppointmentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class AppointmentService {
    private final AppointmentRepository repository;
    private final AppointmentEventProducer eventProducer;

    public AppointmentService(AppointmentRepository repository, AppointmentEventProducer eventProducer) {
        this.repository = repository; this.eventProducer = eventProducer;
    }

    public List<AppointmentResponse> list(LocalDate date, Long patientId, Long doctorId, AppointmentStatus status) {
        OffsetDateTime from = date == null ? null : date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = from == null ? null : from.plusDays(1);
        Specification<Appointment> filters = (root, query, builder) -> builder.conjunction();
        if (patientId != null) filters = filters.and((root, query, builder) -> builder.equal(root.get("patientId"), patientId));
        if (doctorId != null) filters = filters.and((root, query, builder) -> builder.equal(root.get("doctorId"), doctorId));
        if (status != null) filters = filters.and((root, query, builder) -> builder.equal(root.get("status"), status));
        if (from != null) filters = filters.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("appointmentDateTime"), from));
        if (to != null) filters = filters.and((root, query, builder) -> builder.lessThan(root.get("appointmentDateTime"), to));
        return repository.findAll(filters, Sort.by(Sort.Direction.ASC, "appointmentDateTime"))
                .stream().map(AppointmentResponse::from).toList();
    }

    public AppointmentResponse get(Long id) { return AppointmentResponse.from(find(id)); }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request, String actorId) {
        ensureAvailable(-1L, request);
        Appointment appointment = new Appointment(request.patientId(), request.doctorId(),
                request.appointmentDateTime(), request.durationMinutes(), request.reason().trim(), clean(request.notes()));
        saveWithoutConflict(appointment);
        eventProducer.publish("AppointmentScheduled", appointment, actorId);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request, String actorId) {
        Appointment appointment = find(id);
        if (isTerminal(appointment.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "APPOINTMENT_FINALIZED",
                    "A completed, cancelled, or no-show appointment cannot be rescheduled");
        }
        boolean scheduleChanged = !appointment.getDoctorId().equals(request.doctorId())
                || !appointment.getPatientId().equals(request.patientId())
                || !appointment.getAppointmentDateTime().isEqual(request.appointmentDateTime())
                || !appointment.getDurationMinutes().equals(request.durationMinutes());
        if (scheduleChanged) ensureAvailable(id, request);
        appointment.reschedule(request.patientId(), request.doctorId(), request.appointmentDateTime(),
                request.durationMinutes(), request.reason().trim(), clean(request.notes()));
        saveWithoutConflict(appointment);
        if (scheduleChanged) eventProducer.publish("AppointmentRescheduled", appointment, actorId);
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public AppointmentResponse changeStatus(Long id, AppointmentStatus nextStatus, String actorId) {
        Appointment appointment = find(id);
        AppointmentStatus current = appointment.getStatus();
        if (current == nextStatus) return AppointmentResponse.from(appointment);
        if (!allowedTransitions(current).contains(nextStatus)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION",
                    "Appointment status cannot change from " + current + " to " + nextStatus);
        }
        appointment.changeStatus(nextStatus);
        saveWithoutConflict(appointment);
        if (nextStatus == AppointmentStatus.CANCELLED) {
            eventProducer.publish("AppointmentCancelled", appointment, actorId);
        } else if (nextStatus == AppointmentStatus.COMPLETED) {
            eventProducer.publish("AppointmentCompleted", appointment, actorId);
        }
        return AppointmentResponse.from(appointment);
    }

    @Transactional
    public void cancel(Long id, String actorId) {
        Appointment appointment = find(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) return;
        if (!allowedTransitions(appointment.getStatus()).contains(AppointmentStatus.CANCELLED)) {
            throw new ApiException(HttpStatus.CONFLICT, "APPOINTMENT_FINALIZED",
                    "Completed and no-show appointments cannot be cancelled");
        }
        appointment.changeStatus(AppointmentStatus.CANCELLED);
        saveWithoutConflict(appointment);
        eventProducer.publish("AppointmentCancelled", appointment, actorId);
    }

    private Appointment find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment " + id + " was not found"));
    }

    private void ensureAvailable(Long excludedId, AppointmentRequest request) {
        OffsetDateTime end = request.appointmentDateTime().plusMinutes(request.durationMinutes());
        if (repository.existsSchedulingConflict(excludedId, request.patientId(), request.doctorId(),
                request.appointmentDateTime(), end)) {
            throw conflict();
        }
    }

    private void saveWithoutConflict(Appointment appointment) {
        try {
            repository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException exception) {
            throw conflict();
        }
    }

    private ApiException conflict() {
        return new ApiException(HttpStatus.CONFLICT, "APPOINTMENT_CONFLICT",
                "The doctor or patient already has an overlapping appointment");
    }

    private Set<AppointmentStatus> allowedTransitions(AppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED,
                    AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);
            case CONFIRMED -> EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED,
                    AppointmentStatus.NO_SHOW);
            case COMPLETED, CANCELLED, NO_SHOW -> EnumSet.noneOf(AppointmentStatus.class);
        };
    }

    private boolean isTerminal(AppointmentStatus status) {
        return status == AppointmentStatus.COMPLETED || status == AppointmentStatus.CANCELLED
                || status == AppointmentStatus.NO_SHOW;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
