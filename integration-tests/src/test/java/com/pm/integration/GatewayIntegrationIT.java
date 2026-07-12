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
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
        runDockerCompose("up", "-d", "--build", "--wait");

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
                .statusCode(401);
    }

    @Test
    void getPatientsWithValidJwtReturnsPatients() {
        String token = loginAndGetToken();

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/patients")
        .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].email", equalTo("integration.patient@example.com"));
    }

    @Test
    void getPatientsWithoutTokenReturnsUnauthorizedFromGateway() {
        given()
        .when()
                .get("/api/patients")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("message", equalTo("Missing or invalid Authorization header"));
    }

    private String loginAndGetToken() {
        return given()
                .contentType("application/json")
                .body("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD))
        .when()
                .post("/auth/login")
        .then()
                .statusCode(200)
                .extract()
                .path("token");
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
