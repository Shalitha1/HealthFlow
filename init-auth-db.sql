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