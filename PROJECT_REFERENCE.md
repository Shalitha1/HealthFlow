# HealthFlow — Project Reference

> **Purpose:** a practical, repository-aligned reference for the HealthFlow healthcare operations platform. This document describes the code and configuration currently present in this repository, and calls out where that differs from the Milestone 1 requirements.

## 1. What HealthFlow is

HealthFlow is an internal healthcare-operations application. Authorized staff use a browser application to manage patients, appointments, billing and audit history. The browser talks only to the API Gateway; the gateway routes requests to independently deployable Spring Boot services.

The platform is designed around these business roles:

| Role | Intended responsibilities |
| --- | --- |
| `ADMIN` | System dashboard, read-only patient/appointment/billing access, audit viewing, and future staff-user management. |
| `RECEPTIONIST` | Register and update patients; schedule, reschedule, and cancel appointments. |
| `DOCTOR` | View assigned appointments and eventually add protected clinical notes / complete appointments. |
| `BILLING` | View billing, create invoices, and record payments. |

The initial organization time zone is `Asia/Colombo`; billing starts in Sri Lankan rupees (`LKR`). Development/demo data must be used only—this project is not approved for real patient data.

## 2. Repository layout

```text
Springboot/
├── api-gateway/              Spring Cloud Gateway, port 8084
├── auth-service/             Authentication and JWT issuance, port 8085
├── patient-management/       Patient Service, port 8080
├── appointment-service/       Appointment Service, port 8086
├── billing-service/           Billing Service, port 8081
├── audit-service/             Kafka audit consumer and read API, port 8083
├── notification-service/      Kafka notification consumer, port 8082
├── frontend/                  React + TypeScript + Vite web application
├── integration-tests/         Docker-based gateway integration test setup
├── docker/postgres/init/      Database initialization SQL
├── Project Documents/         Requirements and implementation planning
└── docker-compose.yml         Local full-stack orchestration
```

Each backend service has its own Maven wrapper, `pom.xml`, `Dockerfile`, Spring configuration, Flyway migrations, and a basic Spring Boot application-context test. There is no Maven parent/aggregator project at repository root.

## 3. Architecture

```text
Browser
  │ http://localhost:3000
  ▼
React frontend ────── HTTP + Bearer JWT ──────► API Gateway :8084
                                                       │
     ┌───────────────┬──────────────┬──────────┬───────┴───────┐
     ▼               ▼              ▼          ▼               ▼
 Auth :8085      Patient :8080  Billing :8081  Appointment   Audit :8083
                                                     :8086
                       │             │             │
                       └──── Kafka event producers ┘
                                         │
                    patient-events / billing-events / appointment-events
                                         │
                                  ┌──────┴───────┐
                                  ▼              ▼
                            Audit Service   Notification Service :8082
                                  │
                            PostgreSQL databases
```

### Architectural principles currently used

- The API Gateway is the only browser-facing backend port. Internal service ports are Docker `expose`d rather than published to the host.
- Services own separate PostgreSQL databases: `auth_db`, `patient_db`, `appointment_db`, `billing_db`, `audit_db`, and `notification_db`.
- Flyway owns schema evolution. Hibernate uses `ddl-auto=validate`, so it validates rather than creates or changes schema.
- Patient, appointment, and billing mutations produce Kafka events. Audit and notification processing are asynchronous consumers.
- Patient registration synchronously calls Billing Service to create the patient’s billing account, in addition to publishing its event.
- Gateway authentication is implemented by a custom `JwtAuthenticationFilter`, which validates a token through Auth Service and sends identity headers downstream.

## 4. Runtime services and ports

| Component | Host port | Internal port | Responsibility |
| --- | ---: | ---: | --- |
| Frontend | `3000` | `80` | Nginx-served production build of the React app. |
| API Gateway | `8084` | `8084` | Routing, CORS, and JWT/role enforcement. |
| Auth Service | — | `8085` | Login, registration, token validation, and current user lookup. |
| Patient Service | — | `8080` | Patient records, searching, status updates, statistics, event production. |
| Billing Service | — | `8081` | Billing accounts, invoices, payments, statistics, event production. |
| Notification Service | — | `8082` | Consumes patient and appointment events; optional email notification. |
| Audit Service | — | `8083` | Consumes patient, appointment, and billing events; exposes audit history. |
| Appointment Service | — | `8086` | Scheduling, updates, cancellation, status changes, event production. |
| PostgreSQL | `5432` | `5432` | Service databases. |
| Kafka | `29092` | `9092` | Message broker (`29092` is host access; `9092` is Docker-network access). |
| ZooKeeper | `2181` | `2181` | Kafka coordination for the current Confluent Kafka image configuration. |

All Spring services expose Actuator `health` and `info` endpoints. The gateway additionally exposes its `gateway` actuator endpoint. Docker Compose waits for health checks where configured before bringing dependent services online.

## 5. Service-by-service reference

### 5.1 API Gateway

**Module:** `api-gateway`  
**Port:** `8084`  
**Configuration:** `api-gateway/src/main/resources/application.yml`

The gateway uses Spring Cloud Gateway routes to target Docker service names. It allows the configured `FRONTEND_ORIGIN` (default `http://localhost:3000`) with credentialed CORS and common HTTP methods.

Public authentication routes are:

| Route | Target |
| --- | --- |
| `POST /auth/login` | Auth Service |
| `POST /auth/register` | Auth Service |
| `POST /auth/validate` | Auth Service |

`GET /auth/me` is protected by the gateway filter. Gateway also offers proxied OpenAPI documents at `/openapi/auth`, `/openapi/patient`, `/openapi/billing`, `/openapi/appointment`, and `/openapi/audit`.

Protected gateway route policy currently in code:

| Area | Allowed roles |
| --- | --- |
| Patient list and statistics | `ADMIN`, `RECEPTIONIST` |
| Patient details | `ADMIN`, `RECEPTIONIST`, `DOCTOR` |
| Patient creates/updates/status changes | `ADMIN`, `RECEPTIONIST` |
| Patient deletion/deactivation | `ADMIN` |
| Appointment reads | `ADMIN`, `RECEPTIONIST`, `DOCTOR` |
| Appointment create/update/cancel | `ADMIN`, `RECEPTIONIST` |
| Appointment status | `ADMIN`, `RECEPTIONIST`, `DOCTOR` |
| Every billing route | `ADMIN`, `RECEPTIONIST` |
| Audit logs | `ADMIN` |

### 5.2 Auth Service

**Module:** `auth-service`  
**Port:** `8085`  
**Database:** `auth_db`

Auth Service manages staff/user credentials, validates active accounts, generates JWTs, and provides the authenticated user profile. A JWT secret and token expiration are environment-configurable (`JWT_SECRET`, `JWT_EXPIRATION_MS`); Compose sets an expiry of 900,000 milliseconds (15 minutes).

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/auth/login` | Authenticates an active user and returns a JWT. |
| `POST` | `/auth/register` | Creates an account with the `RECEPTIONIST` role and returns a JWT. |
| `POST` | `/auth/validate` | Internal gateway token validation endpoint. |
| `GET` | `/auth/me` | Returns the user represented by a validated token forwarded through the gateway. |

The dev profile includes a development-admin seed migration. Authentication is intentionally token-only: there are no refresh tokens, and expiry requires a new login.

### 5.3 Patient Service

**Module:** `patient-management`  
**Application name:** `patient-service`  
**Port:** `8080`  
**Database:** `patient_db`

Patient Service owns patient identity and demographic data, including name, email, address, date of birth, active state, and timestamps. It supports pagination, text search, active-state filtering, email uniqueness handling, and soft deactivation. Patient creation invokes Billing Service using `BILLING_SERVICE_URL` to create the corresponding billing account, then publishes a `patient-events` Kafka event.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/patients` | Paginated list; accepts `search`, `active`, `page`, and `size` (1–100). |
| `GET` | `/api/patients/statistics` | Patient summary statistics. |
| `GET` | `/api/patients/{id}` | Patient detail. |
| `POST` | `/api/patients` | Register a patient. |
| `PUT` | `/api/patients/{id}` | Replace/update patient details. |
| `PATCH` | `/api/patients/{id}/status` | Activate or deactivate a patient. |
| `DELETE` | `/api/patients/{id}` | Compatibility soft delete; keeps database history. |

### 5.4 Appointment Service

**Module:** `appointment-service`  
**Port:** `8086`  
**Database:** `appointment_db`

Appointment Service stores patient and doctor IDs, start/end time, status, and timestamps. Its current controller supports list filters by date, patient, doctor and status. Scheduling checks are described in the controller/service as preventing double bookings; cancellation retains history by setting `CANCELLED`. Mutations emit `appointment-events`.

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/appointments` | Filter by `date`, `patientId`, `doctorId`, and `status`. |
| `GET` | `/api/appointments/{id}` | Appointment detail. |
| `POST` | `/api/appointments` | Schedule an appointment. |
| `PUT` | `/api/appointments/{id}` | Update/reschedule an appointment. |
| `PATCH` | `/api/appointments/{id}/status` | Change status. |
| `DELETE` | `/api/appointments/{id}` | Cancel (soft cancellation). |

The business document calls for protected clinical notes and a doctor-only completion flow. These specific endpoint and data-protection rules are not evident in the current controller surface and should be verified before treating them as complete.

### 5.5 Billing Service

**Module:** `billing-service`  
**Port:** `8081`  
**Database:** `billing_db`

Billing Service owns billing accounts, invoices, invoice items, payments, invoice status, and account status. The service calculates invoice totals from items; payment processing locks the invoice, prevents overpayment, and recalculates invoice status. It produces `billing-events` for audit consumption.

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/billing/accounts` | Create an account; also called by patient registration. |
| `GET` | `/api/billing/accounts/patient/{patientId}` | Account, invoices, payments and balances for a patient. |
| `POST` | `/api/billing/invoices` | Create an invoice and its items. |
| `GET` | `/api/billing/invoices` | List invoices; optional `status` and `patientId`. |
| `GET` | `/api/billing/invoices/{id}` | Invoice detail with items, payments and balance. |
| `POST` | `/api/billing/invoices/{id}/payments` | Record payment and return recalculated invoice. |
| `GET` | `/api/billing/statistics` | Billing dashboard statistics. |

The documented invoice statuses are `DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, and `VOID`. The documented payment methods are `CASH`, `CARD`, and `BANK_TRANSFER`.

### 5.6 Audit Service

**Module:** `audit-service`  
**Port:** `8083`  
**Database:** `audit_db`

Audit Service has Kafka consumers for patient, appointment and billing topics. It writes audit log records and exposes one read-only endpoint:

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/audit-logs` | Return all audit log records; gateway-restricted to `ADMIN`. |

The service consumes from the earliest available offset for its consumer group. It logs consumer topic, partition, and offset when processing patient events.

### 5.7 Notification Service

**Module:** `notification-service`  
**Port:** `8082`  
**Database:** `notification_db`

Notification Service consumes patient and appointment events. It contains an email notification service and can enable email behavior with `NOTIFICATIONS_EMAIL_ENABLED`; local Compose explicitly disables email. No HTTP controller is currently present, so the service is event-driven rather than frontend-facing.

## 6. Events and cross-service behavior

| Topic | Producer | Consumers | Purpose |
| --- | --- | --- | --- |
| `patient-events` | Patient Service | Audit Service, Notification Service | Record and react to patient lifecycle events. |
| `appointment-events` | Appointment Service | Audit Service, Notification Service | Record and react to appointment lifecycle events. |
| `billing-events` | Billing Service | Audit Service | Record billing lifecycle events. |

### Patient registration flow

1. A permitted caller sends `POST /api/patients` to API Gateway with a bearer token.
2. Gateway validates the JWT through Auth Service and applies its configured role policy.
3. Patient Service validates and persists the patient in `patient_db`.
4. Patient Service calls Billing Service internally to create the billing account.
5. Patient Service publishes a patient event to Kafka.
6. Audit Service stores an audit record; Notification Service handles the event according to its configuration.

### Important reliability implication

The patient-to-billing call and Kafka publication are distributed side effects. The requirements explicitly defer final distributed-failure handling to a reliability milestone. There is no visible transactional outbox or saga orchestration in the repository, so failed downstream work must not be assumed to be automatically reconciled.

## 7. Data, migrations, and persistence

PostgreSQL starts with `patient_db` as the default database and runs `docker/postgres/init/00-create-databases.sql` to create the other service databases if absent. The init script runs only when the Postgres data volume is first initialized.

| Database | Owner service | Migration location |
| --- | --- | --- |
| `auth_db` | Auth Service | `auth-service/src/main/resources/db/migration` |
| `patient_db` | Patient Service | `patient-management/src/main/resources/db/migration` |
| `appointment_db` | Appointment Service | `appointment-service/src/main/resources/db/migration` |
| `billing_db` | Billing Service | `billing-service/src/main/resources/db/migration` |
| `audit_db` | Audit Service | `audit-service/src/main/resources/db/migration` |
| `notification_db` | Notification Service | `notification-service/src/main/resources/db/migration` |

Database files are persisted in the Docker named volume `postgres_data`. Removing that volume resets all local data and causes initialization/migrations to run for fresh databases.

## 8. Frontend

**Module:** `frontend`  
**Stack:** React 19, TypeScript, Vite, React Router, TanStack React Query, React Hook Form, Zod, Tailwind CSS, Lucide icons.

The frontend has an authentication provider and protected-route mechanism. API modules include `authApi`, `patientApi`, `appointmentApi`, `billingApi`, `auditApi`, `notificationApi`, `systemApi`, and a shared HTTP client. Feature pages currently exist for:

- Login and signup
- Dashboard
- Patient list, details and form
- Appointment list, details, form, doctor schedule and status badge
- Billing dashboard, patient billing, invoices, invoice form/details, payments and status badge
- Audit history
- Landing, access denied, not found and coming-soon pages

The production frontend image uses Nginx. Docker builds it with `VITE_API_BASE_URL=http://localhost:8084`, so browser calls target the gateway.

Useful local commands:

```bash
cd frontend
npm install
npm run dev
npm run build
npm run lint
```

## 9. Local development and operations

### Start the whole application

```bash
docker compose up --build
```

Then open `http://localhost:3000`. Use gateway API base URL `http://localhost:8084` for manual browser/API calls. Container/service status and logs can be observed with:

```bash
docker compose ps
docker compose logs -f api-gateway
docker compose logs -f patient-service billing-service
```

### Stop the environment

```bash
docker compose down
```

This preserves the `postgres_data` volume. Only remove the named volume when an intentional local database reset is required.

### Run a backend module locally

Each service has a Maven wrapper. For example:

```bash
cd patient-management
./mvnw spring-boot:run
./mvnw test
```

When running outside Compose, service defaults point at localhost (`PostgreSQL :5432`, Kafka `:9092`, and Billing Service `:8081` for Patient Service). Start dependencies first or set environment variables to match another environment.

### Integration tests

`integration-tests/docker-compose.integration.yml` defines an isolated Compose environment for gateway integration testing, including PostgreSQL, Kafka/ZooKeeper, billing, auth, patient, audit, gateway, and appointment services. Notification Service and frontend are not included in that integration Compose file.

## 10. Configuration reference

| Environment variable | Used by | Meaning / local default |
| --- | --- | --- |
| `DB_HOST`, `DB_PORT` | Database-backed services | PostgreSQL address; defaults to `localhost:5432`. |
| `DB_NAME` | Database-backed services | Service database name. |
| `DB_USERNAME`, `DB_PASSWORD` | Database-backed services | PostgreSQL credentials. |
| `JWT_SECRET` | Auth Service | JWT signing/validation secret. |
| `JWT_EXPIRATION_MS` | Auth Service | Token lifetime; Compose uses 15 minutes. |
| `AUTH_SERVICE_VALIDATE_URL` | API Gateway | Auth token validation endpoint. |
| `FRONTEND_ORIGIN` | API Gateway | Allowed CORS origin. |
| `KAFKA_BOOTSTRAP_SERVERS` | Event producers/consumers | Kafka broker address. |
| `PATIENT_EVENTS_TOPIC` | Audit/Notification | Patient-event topic. |
| `APPOINTMENT_EVENTS_TOPIC` | Appointment/Audit/Notification | Appointment-event topic. |
| `BILLING_EVENTS_TOPIC` | Billing/Audit | Billing-event topic. |
| `KAFKA_CONSUMER_GROUP_ID` | Audit/Notification | Consumer-group identity. |
| `BILLING_SERVICE_URL` | Patient Service | Internal Billing Service base URL. |
| `NOTIFICATIONS_EMAIL_ENABLED` | Notification Service | Turns email behavior on/off; `false` in Compose. |
| `VITE_API_BASE_URL` | Frontend build | Gateway URL compiled into the frontend. |

## 11. Authentication, authorization, and safety

- Login returns a JWT. The gateway validates it for protected routes and forwards an `X-User-Id` header to services that use actor attribution.
- Backend access control is gateway-based. A frontend must still hide unavailable navigation/actions, but hiding UI is not authorization.
- In local Compose, database credentials and a JWT secret are deliberately development defaults. Replace them through secret management for any shared, staging, or production environment.
- JWTs, passwords, secrets, protected clinical notes, and unnecessary patient data must not be written to logs.
- The requirements prefer deactivation over hard deletion. Patient deletion is implemented as a compatibility soft-delete operation, but it should not be exposed in the UI until a retention/deactivation policy is approved.
- Use HTTPS, secure deployment configuration, backups, retention rules, privacy controls, and jurisdiction-specific compliance review before any real healthcare deployment.

## 12. Requirements versus present implementation

`Project Documents/Milestone 1 - Application Requirements.md` is the functional source of intent, but parts describe an earlier repository state. The following are the most important known differences:

| Topic | Requirement | Current code/configuration |
| --- | --- | --- |
| Appointment/Billing availability | Document labels most endpoints planned and says Appointment Service is absent. | Appointment Service, Billing Service, routes, migrations, and frontend pages are present. |
| Patient writes | Only Receptionist may register/update patients; Admin is read-only. | Gateway allows both `ADMIN` and `RECEPTIONIST`. |
| Appointment mutations | Receptionist schedules/reschedules/cancels; doctor completes assigned work. | Gateway allows `ADMIN` and `RECEPTIONIST` to create/update/cancel; status changes also allow `DOCTOR`. |
| Billing access | Billing staff mutate; Admin is read-only. | Gateway currently allows `ADMIN` and `RECEPTIONIST` for every billing endpoint. `BILLING` is not in the gateway billing role list. |
| Staff management | Admin user list/create/role/status endpoints are planned. | No corresponding controller/routes are present. |
| Dashboard summary | `GET /api/dashboard/summary` is planned. | No dashboard backend route is present. |
| Clinical notes | Only assigned doctor may access protected clinical notes. | Dedicated clinical-note endpoint/protection is not visible in current controller mappings. |
| Delete behavior | Do not expose delete patient flow pending policy. | Soft-delete API exists and is gateway accessible to `ADMIN`; frontend exposure should remain avoided. |

Before treating a milestone as complete, decide whether the code or the requirements document is authoritative for each difference, update the other artifact, and add authorization/integration tests for the final policy.

## 13. Recommended verification checklist

1. Start the stack and wait until `docker compose ps` reports healthy services.
2. Verify frontend loading at `http://localhost:3000` and gateway health at `http://localhost:8084/actuator/health`.
3. Log in with controlled development credentials; verify expiry, invalid-login handling, and logout.
4. Through gateway only, create/update a test patient and confirm billing-account creation plus audit/notification consumption.
5. Create, update, cancel, and filter a test appointment; confirm appointment audit events.
6. Create invoice items, record partial payment, and attempt an overpayment to confirm billing safeguards.
7. Test every role against every write and read endpoint, especially the policy differences listed above.
8. Run each service test suite and the integration-test setup after interface, event, route, database, or role-policy changes.
9. Confirm that logs and HTTP responses do not reveal passwords, JWTs, secrets, or excessive patient data.

## 14. Useful source locations

| Need | Location |
| --- | --- |
| Full local stack | `docker-compose.yml` |
| Functional requirements | `Project Documents/Milestone 1 - Application Requirements.md` |
| Gateway routes/CORS | `api-gateway/src/main/resources/application.yml` |
| Gateway JWT filter | `api-gateway/src/main/java/com/pm/apigateway/filter/JwtAuthenticationFilter.java` |
| PostgreSQL database creation | `docker/postgres/init/00-create-databases.sql` |
| Database schemas | Each service’s `src/main/resources/db/migration/` directory |
| API controllers | Each service’s `src/main/java/.../controller/` directory |
| Kafka producers/consumers | Service `event` / `kafka` packages |
| Frontend routes/layout | `frontend/src/App.tsx` and `frontend/src/components/layout/` |
| Frontend API calls | `frontend/src/api/` |
| Integration environment | `integration-tests/docker-compose.integration.yml` |

## 15. Glossary

| Term | Meaning in HealthFlow |
| --- | --- |
| API Gateway | The single backend endpoint used by the browser; routes and authorizes requests. |
| JWT | Signed access token used to identify the authenticated user and role. |
| Flyway | Database migration tool that creates/evolves schemas through versioned SQL. |
| Kafka topic | Named asynchronous event stream, such as `patient-events`. |
| Consumer group | Kafka subscribers sharing offsets and workload under one group identity. |
| Soft delete / deactivation | Marking a record inactive while retaining it for history rather than removing its row. |
| Actuator | Spring Boot operational endpoints, including `/actuator/health`. |
| DTO | Request/response data object used at an API boundary. |

---

This document reflects the repository as inspected on 2026-09-07. Keep it updated when routes, roles, events, schemas, infrastructure, or requirements change.
