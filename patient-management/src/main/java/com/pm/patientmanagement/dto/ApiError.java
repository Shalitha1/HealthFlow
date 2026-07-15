package com.pm.patientmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard API error response")
public record ApiError(
        @Schema(example = "2026-07-15T10:15:30Z") Instant timestamp,
        @Schema(example = "400") int status,
        @Schema(example = "VALIDATION_FAILED") String errorCode,
        @Schema(example = "Input validation failed") String message,
        @Schema(example = "/api/patients") String path,
        Map<String, String> fieldErrors
) {
}
