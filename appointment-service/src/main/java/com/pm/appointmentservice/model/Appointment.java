package com.pm.appointmentservice.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long patientId;
    @Column(nullable = false) private Long doctorId;
    @Column(nullable = false) private OffsetDateTime appointmentDateTime;
    @Column(nullable = false) private Integer durationMinutes;
    @Column(nullable = false, length = 500) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private AppointmentStatus status;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(nullable = false) private OffsetDateTime updatedAt;
    @Version @Column(nullable = false) private Long version;

    protected Appointment() {}

    public Appointment(Long patientId, Long doctorId, OffsetDateTime appointmentDateTime,
                       Integer durationMinutes, String reason, String notes) {
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.appointmentDateTime = appointmentDateTime;
        this.durationMinutes = durationMinutes;
        this.reason = reason;
        this.notes = notes;
        this.status = AppointmentStatus.SCHEDULED;
    }

    @PrePersist void onCreate() { createdAt = OffsetDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public Long getDoctorId() { return doctorId; }
    public OffsetDateTime getAppointmentDateTime() { return appointmentDateTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void reschedule(Long patientId, Long doctorId, OffsetDateTime dateTime,
                           Integer durationMinutes, String reason, String notes) {
        this.patientId = patientId; this.doctorId = doctorId; this.appointmentDateTime = dateTime;
        this.durationMinutes = durationMinutes; this.reason = reason; this.notes = notes;
    }
    public void changeStatus(AppointmentStatus status) { this.status = status; }
}
