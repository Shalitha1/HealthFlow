SELECT 'CREATE DATABASE auth_db OWNER admin'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')
\gexec

SELECT 'CREATE DATABASE patient_db OWNER admin'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'patient_db')
\gexec

SELECT 'CREATE DATABASE appointment_db OWNER admin'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'appointment_db')
\gexec

SELECT 'CREATE DATABASE billing_db OWNER admin'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'billing_db')
\gexec

SELECT 'CREATE DATABASE audit_db OWNER admin'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'audit_db')
\gexec

SELECT 'CREATE DATABASE notification_db OWNER admin'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')
\gexec
