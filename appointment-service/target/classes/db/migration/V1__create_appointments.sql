CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    appointment_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    duration_minutes INTEGER NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    appointment_slot TSTZRANGE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_appointment_duration CHECK (duration_minutes BETWEEN 5 AND 480),
    CONSTRAINT chk_appointment_status CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'))
);

CREATE INDEX idx_appointments_date_time ON appointments(appointment_date_time);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);
CREATE INDEX idx_appointments_doctor ON appointments(doctor_id);
CREATE INDEX idx_appointments_status ON appointments(status);

CREATE OR REPLACE FUNCTION set_appointment_slot() RETURNS TRIGGER AS $$
BEGIN
    NEW.appointment_slot = tstzrange(
        NEW.appointment_date_time,
        NEW.appointment_date_time + NEW.duration_minutes * INTERVAL '1 minute',
        '[)'
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER appointments_set_slot
    BEFORE INSERT OR UPDATE OF appointment_date_time, duration_minutes
    ON appointments
    FOR EACH ROW EXECUTE FUNCTION set_appointment_slot();

ALTER TABLE appointments ADD CONSTRAINT no_doctor_double_booking
    EXCLUDE USING gist (
        doctor_id WITH =,
        appointment_slot WITH &&
    ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));

ALTER TABLE appointments ADD CONSTRAINT no_patient_double_booking
    EXCLUDE USING gist (
        patient_id WITH =,
        appointment_slot WITH &&
    ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));
