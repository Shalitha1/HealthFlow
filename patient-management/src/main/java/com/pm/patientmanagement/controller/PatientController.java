package com.pm.patientmanagement.controller;

import com.pm.patientmanagement.dto.ApiError;
import com.pm.patientmanagement.dto.PatientPageResponseDTO;
import com.pm.patientmanagement.dto.PatientRequestDTO;
import com.pm.patientmanagement.dto.PatientResponseDTO;
import com.pm.patientmanagement.dto.PatientStatisticsDTO;
import com.pm.patientmanagement.dto.PatientStatusUpdateRequestDTO;
import com.pm.patientmanagement.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

/**
 * Patient REST controller
 */
@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients", description = "Patient registration and management")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class PatientController {

    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    @Autowired
    private PatientService patientService;

    /**
     * GET /api/patients
     * Get all patients
     */
    @GetMapping
    @Operation(summary = "List patients", description = "Required role: ADMIN or RECEPTIONIST")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patients returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Required role missing", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientPageResponseDTO> getPatients(
            @RequestParam(required = false)
            @Size(max = 100, message = "Search must not exceed 100 characters")
            String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be zero or greater")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "Page size must be at least 1")
            @Max(value = 100, message = "Page size must not exceed 100")
            int size
    ) {
        logger.info(
                "GET /api/patients - search={}, active={}, page={}, size={}",
                search,
                active,
                page,
                size
        );
        return ResponseEntity.ok(patientService.getPatients(search, active, page, size));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get patient statistics", description = "Required role: ADMIN or RECEPTIONIST")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient statistics returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN or RECEPTIONIST role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientStatisticsDTO> getStatistics() {
        return ResponseEntity.ok(patientService.getStatistics());
    }

    /**
     * GET /api/patients/{id}
     * Get patient by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get patient", description = "Required role: ADMIN, RECEPTIONIST, or authorized DOCTOR")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Required role missing", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable Long id) {
        logger.info("GET /api/patients/{}", id);
        PatientResponseDTO patient = patientService.getPatientById(id);
        return ResponseEntity.ok(patient);
    }

    /**
     * POST /api/patients
     * Create new patient
     */
    @PostMapping
    @Operation(summary = "Register patient", description = "Required role: ADMIN or RECEPTIONIST")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Patient registered"),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN or RECEPTIONIST role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientResponseDTO> createPatient(
            @Valid @RequestBody PatientRequestDTO requestDTO) {
        logger.info("POST /api/patients - Creating patient: {}", requestDTO.getEmail());
        PatientResponseDTO patient = patientService.createPatient(requestDTO);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(patient);
    }

    /**
     * PUT /api/patients/{id}
     * Update patient
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update patient", description = "Required role: ADMIN or RECEPTIONIST")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN or RECEPTIONIST role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientResponseDTO> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequestDTO requestDTO) {
        logger.info("PUT /api/patients/{}", id);
        PatientResponseDTO patient = patientService.updatePatient(id, requestDTO);
        return ResponseEntity.ok(patient);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update patient status", description = "Soft-deactivates or reactivates a patient; required role: ADMIN or RECEPTIONIST")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Patient status updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN or RECEPTIONIST role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PatientResponseDTO> updatePatientStatus(
            @PathVariable Long id,
            @Valid @RequestBody PatientStatusUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(patientService.updatePatientStatus(id, request.active()));
    }

    /**
     * DELETE /api/patients/{id}
     * Delete patient
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate patient", description = "Compatibility soft-delete operation; the database row is retained. Required role: ADMIN")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Patient marked inactive"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Patient not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        logger.info("DELETE /api/patients/{}", id);
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

}
