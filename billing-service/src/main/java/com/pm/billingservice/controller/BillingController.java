package com.pm.billingservice.controller;

import com.pm.billingservice.dto.ApiError;
import com.pm.billingservice.dto.CreateBillingRequest;
import com.pm.billingservice.dto.CreateBillingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "Patient billing-account operations")
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);

    @PostMapping("/accounts")
    @Operation(summary = "Create billing account", description = "Required role: ADMIN or RECEPTIONIST; also used by the internal patient-registration workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Billing account created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CreateBillingResponse> createBillingAccount(
            @Valid @RequestBody CreateBillingRequest request
    ) {
        String accountId = UUID.randomUUID().toString();

        logger.info("Creating billing account for patient: {}", request.patientId());
        logger.info("Patient name: {}", request.name());
        logger.info("Patient email: {}", request.email());
        logger.info("Assigned billing account ID: {}", accountId);

        CreateBillingResponse response = new CreateBillingResponse(accountId, "ACTIVE");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
