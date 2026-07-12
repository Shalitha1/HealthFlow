CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    address VARCHAR(500),
    date_of_birth DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    actor_id VARCHAR(100),
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_patients_email ON patients(email);
CREATE INDEX IF NOT EXISTS idx_patients_created_at ON patients(created_at);
CREATE INDEX IF NOT EXISTS idx_patients_active ON patients(active);

INSERT INTO patients (name, email, address, date_of_birth, created_at, updated_at, active)
VALUES (
    'Integration Patient',
    'integration.patient@example.com',
    '123 Test Street',
    DATE '1990-01-01',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    TRUE
)
ON CONFLICT (email) DO NOTHING;

CREATE DATABASE auth_db;

\connect auth_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

INSERT INTO users (email, password, role)
VALUES (
    'admin@pm.com',
    '$2y$10$17by3kSLqaWYjZKVv.Qgru/zP8Rf0mxa21uYGTQ4HMfGtBEiThlGS',
    'ADMIN'
)
ON CONFLICT (email) DO NOTHING;
