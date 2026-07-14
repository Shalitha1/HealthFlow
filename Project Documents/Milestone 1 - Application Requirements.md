# HealthFlow - Milestone 1 Application Requirements

## 1. Purpose

HealthFlow is an internal healthcare operations application. It allows authorized staff to manage patients, appointments, billing, staff access, and audit history through a browser frontend that communicates with the backend through the API Gateway.

This document defines what the application must do. It does not require all missing endpoints or frontend pages to be implemented during Milestone 1.

## 2. Delivery scope

### First frontend increment

Start with the `ADMIN` role and build only:

1. Login and logout.
2. Application navigation.
3. Dashboard shell.
4. Read-only patient list and patient details.
5. Read-only billing, appointment, and audit views when their APIs are available.
6. Staff-user management when its API is available.

### Later increments

Add the `RECEPTIONIST`, `DOCTOR`, and `BILLING` roles after the basic frontend and login flow work. The full requirements for those roles are still documented below so later work has a defined target.

## 3. Users and permissions

| Capability | Administrator | Receptionist | Doctor | Billing staff |
|---|:---:|:---:|:---:|:---:|
| Manage staff users | Yes | No | No | No |
| View patients | Yes | Yes | Assigned/needed patients | Billing identity only |
| Register and update patients | No | Yes | No | No |
| View appointments | Yes | Yes | Assigned only | No |
| Schedule, reschedule, or cancel appointments | No | Yes | No | No |
| Complete appointments and add clinical notes | No | No | Assigned only | No |
| View billing | Yes | No | No | Yes |
| Create invoices and record payments | No | No | No | Yes |
| View audit logs | Yes | No | No | No |
| View system dashboard | Yes | No | No | No |

Rules:

- A user must authenticate before opening protected pages.
- The frontend must hide actions the signed-in user cannot perform.
- The backend remains the authority and must reject unauthorized calls even if a user manually constructs a request.
- Disabled users must not be able to sign in or continue using the system after their access expires.
- Patient deletion is not part of the stated business workflow. Do not expose the existing `DELETE /api/patients/{id}` endpoint in the UI until a deletion, deactivation, and retention policy is approved.

## 4. Functional requirements

### FR-01 Authentication

- A staff member can sign in with an email address and password.
- Successful login returns a JWT and opens the appropriate landing page.
- Invalid credentials display a safe error without revealing whether an email exists.
- The application signs the user out when the token is missing or expired.
- Logout clears the locally held authentication data.
- The initial version does not use refresh tokens. After token expiry, the user must sign in again.

Acceptance examples:

- Given valid credentials, when the user submits the login form, then the application opens an authorized page.
- Given invalid credentials, when the user submits the form, then the application stays on the login page and shows "Invalid email or password."

### FR-02 Staff-user administration

- An administrator can list staff users.
- An administrator can create a staff user with a role.
- An administrator can change a staff user's role.
- An administrator can activate or deactivate a staff user.
- Passwords must never be displayed after creation.

### FR-03 Patient management

- An authorized user can list patients and open a patient's details.
- A receptionist can register a patient using name, email, address, and date of birth.
- A receptionist can update a patient's details.
- The form must enforce required fields, valid email format, and a date of birth in the past.
- Successful patient registration automatically creates a billing account and produces audit and notification events.
- A patient email address is unique in the initial version. An existing email produces `409 Conflict` and instructs the user to review the existing patient.

### FR-04 Appointment management

- A receptionist can view appointments and schedule an appointment for a patient and doctor.
- A receptionist can reschedule or cancel an appointment.
- A doctor can view only assigned appointments.
- A doctor can complete an assigned appointment and add clinical notes.
- Appointment times are interpreted in the organization's configured time zone, initially `Asia/Colombo`.
- The default appointment duration is 30 minutes. Receptionists may choose another positive duration.
- Appointments may be scheduled Monday through Friday from 08:00 through 17:00. Store these hours as configuration rather than hard-coded UI rules.
- Two active appointments assigned to the same doctor must not overlap. A conflict returns `409 Conflict`.
- The system must reject an end time that is not after the start time.
- Only the assigned doctor can read or change clinical notes. Administrators and receptionists can see appointment status and scheduling data but not clinical-note content.

### FR-05 Billing

- A billing staff member can view a patient's billing account.
- A billing staff member can create an invoice.
- A billing staff member can record a payment.
- A billing staff member can list unpaid invoices.
- An administrator has read-only access to billing information.
- The initial currency is Sri Lankan rupees (`LKR`).
- Invoice statuses are `DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, and `VOID`.
- Supported payment methods are `CASH`, `CARD`, and `BANK_TRANSFER`.
- Partial payments are allowed. The total recorded payments must not exceed the invoice balance.

### FR-06 Audit history

- An administrator can view audit events.
- An audit record identifies the event type, time, acting user or service, affected resource, and outcome.
- Audit records are read-only in the application.

### FR-07 Dashboard

- An administrator can view a summary of patient, appointment, billing, and service-health information.
- If a dependency is unavailable, the dashboard shows a degraded or unavailable state instead of presenting stale information as current.

## 5. Pages and backend endpoint map

All browser calls should use the API Gateway base URL, currently `http://localhost:8084` in local development.

| Page | Role | Required endpoint(s) | Repository status |
|---|---|---|---|
| Login | All | `POST /auth/login` | Exists |
| Dashboard | Administrator | `GET /api/dashboard/summary` | Planned |
| Staff users | Administrator | `GET /api/users` | Planned |
| Create staff user | Administrator | `POST /api/users` | Planned |
| Edit staff access | Administrator | `PATCH /api/users/{id}/role`, `PATCH /api/users/{id}/status` | Planned |
| Patient list | Administrator, Receptionist | `GET /api/patients` | Exists |
| Patient details | Administrator, Receptionist, authorized Doctor | `GET /api/patients/{id}` | Exists |
| Register patient | Receptionist | `POST /api/patients` | Exists |
| Edit patient | Receptionist | `PUT /api/patients/{id}` | Exists |
| Appointment list | Administrator, Receptionist | `GET /api/appointments` | Planned; appointment service is absent |
| Schedule appointment | Receptionist | `POST /api/appointments` | Planned |
| Appointment details/edit | Receptionist | `GET /api/appointments/{id}`, `PATCH /api/appointments/{id}` | Planned |
| Doctor work list | Doctor | `GET /api/appointments?doctorId={currentUserId}&status=...` | Planned |
| Complete appointment | Doctor | `POST /api/appointments/{id}/completion` | Planned |
| Billing accounts | Administrator, Billing staff | `GET /api/billing/accounts`, `GET /api/billing/accounts/{patientId}` | Planned |
| Invoice list/details | Administrator, Billing staff | `GET /api/billing/invoices`, `GET /api/billing/invoices/{id}` | Planned |
| Create invoice | Billing staff | `POST /api/billing/invoices` | Planned |
| Record payment | Billing staff | `POST /api/billing/invoices/{id}/payments` | Planned |
| Audit history | Administrator | `GET /audit-logs` | Exists |
| Access denied | Any signed-in user | No endpoint; display HTTP `403` result | Frontend concern |

Current backend notes:

- `POST /api/billing/accounts` exists, but it currently generates a response without persisted billing data. Treat it as an internal patient-creation dependency, not yet as a complete billing page API.
- The JWT contains a `role` claim, but `POST /auth/login` returns only the token. The frontend may decode the claim for navigation; authorization must still be enforced by the backend.
- No appointment service or appointment routes are currently present.
- No staff-user management endpoints are currently present.

## 6. Main user workflow

### End-to-end happy path

1. A staff member signs in.
2. A receptionist registers a patient.
3. Patient Service saves the patient.
4. Billing Service creates the patient's billing account automatically.
5. The system publishes audit and notification events.
6. A receptionist schedules an appointment with a doctor.
7. The doctor views the assigned appointment, adds clinical notes, and completes it.
8. Billing staff creates or reviews an invoice and records payment when received.
9. An administrator reviews the audit history.

### Expected failure behavior to define in later milestones

- Invalid form data remains visible and is linked to the relevant form fields.
- An unauthorized operation returns `403 Forbidden` and does not change data.
- A missing record returns `404 Not Found`.
- A scheduling or duplicate-data conflict returns `409 Conflict`.
- A temporary downstream-service failure returns a clear retryable error; it must not look like a successful operation.
- Distributed failure handling for patient creation, billing, Kafka, audit, and notifications is finalized in the reliability milestone.

## 7. Minimum data requirements

### Patient

- ID
- Name
- Email
- Address
- Date of birth
- Active status
- Created and updated timestamps

### Staff user

- ID
- Name
- Email
- Role
- Active status
- Created and updated timestamps

### Appointment

- ID
- Patient ID
- Doctor user ID
- Start and end time
- Status (`SCHEDULED`, `COMPLETED`, `CANCELLED`)
- Clinical notes or a reference to protected clinical notes
- Created and updated timestamps

### Invoice/payment

- Billing account ID
- Patient ID
- Invoice ID
- Amount, currency, due date, and status
- Payment ID, paid amount, payment time, and payment method/reference

### Audit event

- ID
- Event type
- Actor ID or service name
- Resource type and ID
- Timestamp
- Outcome

## 8. Non-functional requirements relevant to the definition

- Use the API Gateway as the only browser-facing backend entry point.
- Use HTTPS and secure token handling outside local development.
- Do not expose passwords, JWT secrets, clinical notes, or unnecessary patient data in logs.
- Validate input on both the frontend and backend.
- Use consistent loading, empty, success, validation-error, forbidden, and server-error states on every page.
- Record important mutations in the audit stream.
- Prefer deactivation over destructive deletion for staff and patient records until retention requirements are agreed.

## 9. Approved development decisions

The following decisions remove ambiguity for development. They must be reviewed again before using HealthFlow with real patient data.

1. Patient email is the unique duplicate-detection key for the initial version.
2. Administrators are read-only for patient, appointment, and billing information. They can mutate staff-user access only.
3. Appointments use the configured organization time zone (`Asia/Colombo` initially), a 30-minute default duration, Monday-Friday working hours of 08:00-17:00, and no overlapping active appointments for one doctor.
4. Only the assigned doctor can view or edit clinical notes. Store clinical notes as protected data that is not returned in general appointment-list responses.
5. Billing starts with `LKR`, allows partial payments, and accepts cash, card, or bank transfer. The statuses listed in FR-05 form the initial invoice lifecycle.
6. Development and demonstration data is retained indefinitely and is deactivated instead of hard-deleted. Real patient data must not be entered until an owner approves jurisdiction-specific privacy, retention, deletion, backup, and audit requirements.
7. Token expiry requires a new login. Refresh tokens are outside the initial scope.

## 10. Milestone 1 completion checklist

- [x] Roles and permissions are documented.
- [x] Required pages are listed.
- [x] Every page is mapped to an existing or planned backend endpoint.
- [x] The main end-to-end user workflow is documented.
- [x] Initial development decisions are recorded.
- [x] Real-data compliance requirements are explicitly deferred and real patient data is prohibited until approval.
- [x] The document is committed on the `healthflow-frontend` development branch.
