package com.pm.appointmentservice.repository;

import com.pm.appointmentservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    @Query(value = """
            SELECT EXISTS (
              SELECT 1 FROM appointments a
              WHERE a.id <> :excludedId
                AND a.status IN ('SCHEDULED', 'CONFIRMED')
                AND (a.doctor_id = :doctorId OR a.patient_id = :patientId)
                AND a.appointment_date_time < :endTime
                AND a.appointment_date_time + a.duration_minutes * INTERVAL '1 minute' > :startTime
            )
            """, nativeQuery = true)
    boolean existsSchedulingConflict(
            @Param("excludedId") Long excludedId,
            @Param("patientId") Long patientId,
            @Param("doctorId") Long doctorId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}
