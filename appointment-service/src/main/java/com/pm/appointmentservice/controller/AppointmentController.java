package com.pm.appointmentservice.controller;

import com.pm.appointmentservice.dto.*;
import com.pm.appointmentservice.model.AppointmentStatus;
import com.pm.appointmentservice.service.AppointmentService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Schedule, filter, reschedule, and complete appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {
    private final AppointmentService service;
    public AppointmentController(AppointmentService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "List appointments", description = "Filter by UTC date, patient, doctor, and status. Required role: ADMIN, RECEPTIONIST, or DOCTOR")
    public List<AppointmentResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) AppointmentStatus status) {
        return service.list(date, patientId, doctorId, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment", description = "Required role: ADMIN, RECEPTIONIST, or DOCTOR")
    public AppointmentResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedule appointment", description = "Prevents patient and doctor double booking. Required role: ADMIN or RECEPTIONIST")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment scheduled"),
            @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Scheduling conflict", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String actorId) {
        return service.create(request, actorId);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update or reschedule appointment", description = "Required role: ADMIN or RECEPTIONIST")
    public AppointmentResponse update(@PathVariable Long id, @Valid @RequestBody AppointmentRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String actorId) {
        return service.update(id, request, actorId);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change appointment status", description = "Required role: ADMIN, RECEPTIONIST, or DOCTOR")
    public AppointmentResponse status(@PathVariable Long id, @Valid @RequestBody AppointmentStatusRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String actorId) {
        return service.changeStatus(id, request.status(), actorId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel appointment", description = "Retains history by changing status to CANCELLED. Required role: ADMIN or RECEPTIONIST")
    public void delete(@PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String actorId) {
        service.cancel(id, actorId);
    }
}
