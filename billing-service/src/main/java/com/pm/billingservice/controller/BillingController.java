package com.pm.billingservice.controller;

import com.pm.billingservice.dto.ApiError;
import com.pm.billingservice.dto.*;
import com.pm.billingservice.model.InvoiceStatus;
import com.pm.billingservice.service.BillingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
@Tag(name = "Billing", description = "Patient billing-account operations")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation or malformed request", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "403", description = "Role is not permitted", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Billing account or invoice not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Billing state conflict", content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class BillingController {
    private final BillingService service;
    public BillingController(BillingService service) { this.service = service; }

    @PostMapping("/accounts")
    @Operation(summary = "Create billing account", description = "Required role: ADMIN or RECEPTIONIST; also used by the internal patient-registration workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Billing account created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<CreateBillingResponse> createBillingAccount(
            @Valid @RequestBody CreateBillingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actorId
    ) {
        BillingAccountResponse account = service.createAccount(request, actorId);
        CreateBillingResponse response = new CreateBillingResponse(account.id(), account.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/accounts/patient/{patientId}")
    @Operation(summary = "Get patient billing overview", description = "Returns the persisted account, invoices, payments, and calculated balances. Required role: ADMIN or RECEPTIONIST")
    public PatientBillingResponse getPatientBilling(@PathVariable String patientId) {
        return service.patientBilling(patientId);
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create invoice", description = "Totals are calculated from invoice items. Required role: ADMIN or RECEPTIONIST")
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actorId) {
        return service.createInvoice(request, actorId);
    }

    @GetMapping("/invoices")
    @Operation(summary = "List invoices", description = "Optionally filter by status or patient ID. Required role: ADMIN or RECEPTIONIST")
    public List<InvoiceResponse> listInvoices(@RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) String patientId) {
        return service.list(status, patientId);
    }

    @GetMapping("/invoices/{id}")
    @Operation(summary = "Get invoice details", description = "Includes line items, payments, and calculated balance. Required role: ADMIN or RECEPTIONIST")
    public InvoiceResponse getInvoice(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record payment", description = "Locks the invoice, prevents overpayment, and recalculates status. Required role: ADMIN or RECEPTIONIST")
    public InvoiceResponse recordPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actorId) {
        return service.recordPayment(id, request, actorId);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get billing dashboard statistics", description = "Required role: ADMIN or RECEPTIONIST")
    public BillingStatisticsResponse statistics() { return service.statistics(); }
}
