package com.pm.integration;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;

class GatewayIntegrationIT {
    private static final String COMPOSE_FILE = "docker-compose.integration.yml";
    private static final String PROJECT_NAME = "pm-integration-tests";
    private static final int GATEWAY_PORT = 8084;
    private static final String ADMIN_EMAIL = "admin@pm.com";
    private static final String ADMIN_PASSWORD = "admin123";

    @BeforeAll
    static void configureRestAssured() {
        if (Boolean.parseBoolean(System.getenv().getOrDefault("PM_INTEGRATION_SKIP_BUILD", "false"))) {
            runDockerCompose("up", "-d", "--wait");
        } else {
            runDockerCompose("up", "-d", "--build", "--wait");
        }

        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = getPublishedGatewayPort();
        waitForGateway();
        waitForAuthService();
    }

    @AfterAll
    static void stopStack() {
        runDockerCompose("down", "-v", "--remove-orphans");
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "admin@pm.com",
                          "password": "admin123"
                        }
                        """)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void loginWithInvalidCredentialsReturnsUnauthorized() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "admin@pm.com",
                          "password": "wrong-password"
                        }
                        """)
        .when()
                .post("/auth/login")
        .then()
                .statusCode(401)
                .body("timestamp", notNullValue())
                .body("status", equalTo(401))
                .body("errorCode", equalTo("AUTH_INVALID_CREDENTIALS"))
                .body("message", equalTo("Invalid email or password"))
                .body("path", equalTo("/auth/login"))
                .body("fieldErrors", notNullValue());
    }

    @Test
    void validateWithValidTokenReturnsJwtClaims() {
        String token = loginAndGetToken();

        given()
                .contentType("application/json")
                .body("""
                        {
                          "token": "%s"
                        }
                        """.formatted(token))
        .when()
                .post("/auth/validate")
        .then()
                .statusCode(200)
                .body("sub", notNullValue())
                .body("role", equalTo("ADMIN"));
    }

    @Test
    void validateWithInvalidTokenReturnsUnauthorized() {
        given()
                .contentType("application/json")
                .body("""
                        {
                          "token": "not-a-valid-jwt"
                        }
                        """)
        .when()
                .post("/auth/validate")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("errorCode", equalTo("AUTH_401"))
                .body("path", equalTo("/auth/validate"));
    }

    @Test
    void getCurrentUserReturnsAuthenticatedUser() {
        String token = loginAndGetToken();

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/auth/me")
        .then()
                .statusCode(200)
                .body("userId", notNullValue())
                .body("email", equalTo(ADMIN_EMAIL))
                .body("role", equalTo("ADMIN"));
    }

    @Test
    void getCurrentUserWithoutTokenReturnsUnauthorized() {
        given()
        .when()
                .get("/auth/me")
        .then()
                .statusCode(401)
                .body("errorCode", equalTo("AUTH_UNAUTHORIZED"))
                .body("path", equalTo("/auth/me"));
    }

    @Test
    void getCurrentUserWithInvalidTokenReturnsUnauthorized() {
        given()
                .header("Authorization", "Bearer not-a-valid-jwt")
        .when()
                .get("/auth/me")
        .then()
                .statusCode(401)
                .body("errorCode", equalTo("AUTH_UNAUTHORIZED"))
                .body("message", equalTo("Invalid or expired token"))
                .body("path", equalTo("/auth/me"));
    }

    @Test
    void doctorRoleCannotReadAdminOnlyAuditLogs() {
        String doctorToken = loginAndGetToken("doctor@pm.com", ADMIN_PASSWORD);

        given()
                .header("Authorization", "Bearer " + doctorToken)
        .when()
                .get("/audit-logs")
        .then()
                .statusCode(403)
                .body("status", equalTo(403))
                .body("errorCode", equalTo("AUTH_FORBIDDEN"))
                .body("message", equalTo("Your role is not allowed to perform this operation"))
                .body("path", equalTo("/audit-logs"));
    }

    @Test
    void gatewayHealthAndInfoEndpointsAreAvailable() {
        given()
        .when()
                .get("/actuator/health")
        .then()
                .statusCode(200)
                .body("status", equalTo("UP"));

        given()
        .when()
                .get("/actuator/info")
        .then()
                .statusCode(200)
                .body("app.name", equalTo("api-gateway"))
                .body("app.version", equalTo("0.0.1-SNAPSHOT"));
    }

    @Test
    void publicOpenApiDocumentsAreAvailableThroughGateway() {
        for (String endpoint : List.of("auth", "patient", "billing", "audit", "appointment")) {
            given()
            .when()
                    .get("/openapi/" + endpoint)
            .then()
                    .statusCode(200)
                    .body("openapi", notNullValue())
                    .body("paths", notNullValue());
        }
    }

    @Test
    void gatewayHandlesFrontendCorsPreflight() {
        given()
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
        .when()
                .options("/api/patients")
        .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", "http://localhost:3000")
                .header("Access-Control-Allow-Credentials", "true");
    }

    @Test
    void invalidPatientRequestReturnsFieldErrorsInStandardFormat() {
        String token = loginAndGetToken();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{}")
        .when()
                .post("/api/patients")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("errorCode", equalTo("VALIDATION_FAILED"))
                .body("path", equalTo("/api/patients"))
                .body("fieldErrors", hasKey("name"))
                .body("fieldErrors", hasKey("email"))
                .body("fieldErrors", hasKey("address"))
                .body("fieldErrors", hasKey("dateOfBirth"));
    }

    @Test
    void getPatientsWithValidJwtReturnsPatients() {
        String token = loginAndGetToken();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "name": "Integration Patient",
                          "email": "integration.patient@example.com",
                          "address": "Integration Test Address",
                          "dateOfBirth": "1990-01-01"
                        }
                        """)
        .when()
                .post("/api/patients")
        .then()
                .statusCode(201)
                .body("email", equalTo("integration.patient@example.com"));

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("patients.size()", greaterThanOrEqualTo(1))
                .body("patients.email", hasItem("integration.patient@example.com"))
                .body("currentPage", equalTo(0))
                .body("pageSize", equalTo(20))
                .body("totalElements", greaterThanOrEqualTo(1));
    }

    @Test
    void patientPaginationSearchFilteringSoftDeletionAndStatisticsWork() {
        String token = loginAndGetToken();
        long johnId = createPatient(
                token,
                "John Milestone Four",
                "john.milestone4@example.com"
        );
        long janeId = createPatient(
                token,
                "Jane Milestone Four",
                "jane.milestone4@example.com"
        );

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("page", 0)
                .queryParam("size", 1)
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("patients.size()", equalTo(1))
                .body("currentPage", equalTo(0))
                .body("pageSize", equalTo(1))
                .body("totalElements", greaterThanOrEqualTo(2))
                .body("totalPages", greaterThanOrEqualTo(2));

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("search", "John Milestone")
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("patients[0].id", equalTo((int) johnId))
                .body("patients[0].name", equalTo("John Milestone Four"));

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("search", "jane.milestone4@example.com")
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("patients[0].id", equalTo((int) janeId));

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("search", johnId)
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("patients[0].id", equalTo((int) johnId));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"active\": false}")
        .when()
                .patch("/api/patients/" + johnId + "/status")
        .then()
                .statusCode(200)
                .body("active", equalTo(false));

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("search", johnId)
                .queryParam("active", false)
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("patients[0].active", equalTo(false));

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("search", johnId)
                .queryParam("active", true)
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("totalElements", equalTo(0))
                .body("patients.size()", equalTo(0));

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/patients/" + janeId)
        .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/patients/" + janeId)
        .then()
                .statusCode(200)
                .body("id", equalTo((int) janeId))
                .body("active", equalTo(false));

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/patients/statistics")
        .then()
                .statusCode(200)
                .body("totalPatients", greaterThanOrEqualTo(2))
                .body("inactivePatients", greaterThanOrEqualTo(2))
                .body("registrationsThisMonth", greaterThanOrEqualTo(2));
    }

    @Test
    void getPatientsWithoutTokenReturnsUnauthorizedFromGateway() {
        given()
        .when()
                .get("/api/patients")
        .then()
                .statusCode(401)
                .body("timestamp", notNullValue())
                .body("status", equalTo(401))
                .body("errorCode", equalTo("AUTH_UNAUTHORIZED"))
                .body("message", equalTo("Missing or invalid Authorization header"))
                .body("path", equalTo("/api/patients"))
                .body("fieldErrors", notNullValue());
    }

    @Test
    void appointmentSchedulingConflictFilteringLifecycleAndAuditEventsWork() {
        String token = loginAndGetToken();
        OffsetDateTime firstTime = OffsetDateTime.now().plusDays(7).withHour(9).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime movedTime = firstTime.plusHours(3);
        long appointmentId = createAppointment(token, 101, 3, firstTime, "Integration consultation");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(appointmentBody(102, 3, firstTime.plusMinutes(15), "Overlapping visit"))
        .when()
                .post("/api/appointments")
        .then()
                .statusCode(409)
                .body("errorCode", equalTo("APPOINTMENT_CONFLICT"));

        given()
                .header("Authorization", "Bearer " + token)
                .queryParam("date", firstTime.toLocalDate().toString())
                .queryParam("doctorId", 3)
                .queryParam("status", "SCHEDULED")
        .when()
                .get("/api/appointments")
        .then()
                .statusCode(200)
                .body("id", hasItem((int) appointmentId));

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(appointmentBody(101, 3, movedTime, "Integration consultation"))
        .when()
                .put("/api/appointments/" + appointmentId)
        .then()
                .statusCode(200)
                .body("appointmentDateTime", notNullValue());

        given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"status\":\"COMPLETED\"}")
        .when()
                .patch("/api/appointments/" + appointmentId + "/status")
        .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));

        long cancellationId = createAppointment(token, 102, 3, firstTime.plusDays(1), "Cancellation test");
        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .delete("/api/appointments/" + cancellationId)
        .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/appointments/" + cancellationId)
        .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));

        waitForAuditEvents(token, List.of(
                "AppointmentScheduled", "AppointmentRescheduled",
                "AppointmentCompleted", "AppointmentCancelled"
        ));
    }

    @Test
    void billingPersistencePaymentCalculationAndAuditEventsWork() {
        String token = loginAndGetToken();
        long patientId = createPatient(token, "Billing Integration Patient", "billing.integration@example.com");

        String accountId = given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/billing/accounts/patient/" + patientId)
        .then()
                .statusCode(200)
                .body("account.patientId", equalTo(String.valueOf(patientId)))
                .body("account.status", equalTo("ACTIVE"))
                .extract().path("account.id");

        long invoiceId = given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "billingAccountId": "%s",
                          "appointmentId": 7001,
                          "dueDate": "%s",
                          "status": "ISSUED",
                          "items": [
                            {"description": "Consultation", "quantity": 2, "unitPrice": 50.00},
                            {"description": "Clinical supplies", "quantity": 1, "unitPrice": 25.00}
                          ]
                        }
                        """.formatted(accountId, java.time.LocalDate.now().plusDays(14)))
        .when()
                .post("/api/billing/invoices")
        .then()
                .statusCode(201)
                .body("invoiceNumber", notNullValue())
                .body("status", equalTo("ISSUED"))
                .body("items.size()", equalTo(2))
                .extract().jsonPath().getLong("id");

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/billing/invoices/" + invoiceId)
        .then().statusCode(200).body("totalAmount", equalTo(125.0f)).body("balance", equalTo(125.0f));

        given().header("Authorization", "Bearer " + token).contentType("application/json")
                .body("{\"amount\":25.00,\"paymentMethod\":\"CARD\",\"reference\":\"PARTIAL-001\"}")
        .when().post("/api/billing/invoices/" + invoiceId + "/payments")
        .then().statusCode(201).body("status", equalTo("PARTIALLY_PAID")).body("balance", equalTo(100.0f));

        given().header("Authorization", "Bearer " + token).contentType("application/json")
                .body("{\"amount\":100.01,\"paymentMethod\":\"CASH\"}")
        .when().post("/api/billing/invoices/" + invoiceId + "/payments")
        .then().statusCode(409).body("errorCode", equalTo("PAYMENT_EXCEEDS_BALANCE"));

        given().header("Authorization", "Bearer " + token).contentType("application/json")
                .body("{\"amount\":100.00,\"paymentMethod\":\"BANK_TRANSFER\",\"reference\":\"FINAL-001\"}")
        .when().post("/api/billing/invoices/" + invoiceId + "/payments")
        .then().statusCode(201).body("status", equalTo("PAID")).body("paidAmount", equalTo(125.0f)).body("balance", equalTo(0.0f));

        given().header("Authorization", "Bearer " + token).queryParam("patientId", patientId)
        .when().get("/api/billing/invoices")
        .then().statusCode(200).body("id", hasItem((int) invoiceId)).body("status", hasItem("PAID"));

        given().header("Authorization", "Bearer " + token)
        .when().get("/api/billing/accounts/patient/" + patientId)
        .then().statusCode(200).body("invoices.size()", equalTo(1)).body("totalPaid", equalTo(125.0f)).body("outstandingBalance", equalTo(0.0f));

        waitForAuditEvents(token, List.of("BillingAccountCreated", "InvoiceIssued", "PaymentRecorded", "InvoicePaid"));
    }

    private String loginAndGetToken() {
        return loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private String loginAndGetToken(String email, String password) {
        return given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(email, password))
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    private long createPatient(String token, String name, String email) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("""
                        {
                          "name": "%s",
                          "email": "%s",
                          "address": "Milestone Four Test Address",
                          "dateOfBirth": "1990-01-01"
                        }
                        """.formatted(name, email))
        .when()
                .post("/api/patients")
        .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private long createAppointment(String token, long patientId, long doctorId,
                                   OffsetDateTime dateTime, String reason) {
        return given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body(appointmentBody(patientId, doctorId, dateTime, reason))
        .when()
                .post("/api/appointments")
        .then()
                .statusCode(201)
                .body("status", equalTo("SCHEDULED"))
                .extract().jsonPath().getLong("id");
    }

    private String appointmentBody(long patientId, long doctorId,
                                   OffsetDateTime dateTime, String reason) {
        return """
                {
                  "patientId": %d,
                  "doctorId": %d,
                  "appointmentDateTime": "%s",
                  "durationMinutes": 30,
                  "reason": "%s",
                  "notes": "Integration test"
                }
                """.formatted(patientId, doctorId, dateTime, reason);
    }

    private void waitForAuditEvents(String token, List<String> eventTypes) {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(45));
        while (Instant.now().isBefore(deadline)) {
            Response response = given().header("Authorization", "Bearer " + token)
                    .when().get("/audit-logs");
            if (response.statusCode() == 200) {
                List<String> recorded = response.jsonPath().getList("eventType");
                if (recorded.containsAll(eventTypes)) return;
            }
            sleep();
        }
        fail("Expected events did not appear in audit history: " + eventTypes);
    }

    private static int getPublishedGatewayPort() {
        String output = runDockerCompose("port", "api-gateway", String.valueOf(GATEWAY_PORT)).trim();
        String port = output.substring(output.lastIndexOf(':') + 1);
        return Integer.parseInt(port);
    }

    private static void waitForGateway() {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(5));

        while (Instant.now().isBefore(deadline)) {
            try {
                given()
                .when()
                        .get("/actuator/health")
                .then()
                        .statusCode(200);
                return;
            } catch (Exception ignored) {
                sleep();
            }
        }

        fail("API Gateway did not become healthy within 5 minutes");
    }

    private static void waitForAuthService() {
        Instant deadline = Instant.now().plus(Duration.ofMinutes(5));

        while (Instant.now().isBefore(deadline)) {
            try {
                Response response = given()
                        .contentType("application/json")
                        .body("""
                                {
                                  "email": "admin@pm.com",
                                  "password": "admin123"
                                }
                                """)
                .when()
                        .post("/auth/login");

                if (response.statusCode() == 200) {
                    return;
                }
            } catch (Exception ignored) {
                sleep();
                continue;
            }

            sleep();
        }

        fail("Auth Service did not accept seeded admin credentials within 5 minutes");
    }

    private static String runDockerCompose(String... args) {
        List<String> command = new java.util.ArrayList<>();
        command.add("docker");
        command.add("compose");
        command.add("-p");
        command.add(PROJECT_NAME);
        command.add("-f");
        command.add(COMPOSE_FILE);
        command.addAll(List.of(args));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new IllegalStateException(String.join(" ", command) + " failed:\n" + output);
            }

            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Could not run Docker Compose", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Docker Compose command was interrupted", e);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(2_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the API Gateway", e);
        }
    }
}
