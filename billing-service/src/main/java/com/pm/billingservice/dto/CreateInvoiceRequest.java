package com.pm.billingservice.dto;
import com.pm.billingservice.model.InvoiceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record CreateInvoiceRequest(
        @NotBlank String billingAccountId,
        @Positive Long appointmentId,
        @NotNull @FutureOrPresent LocalDate dueDate,
        InvoiceStatus status,
        @NotEmpty List<@Valid InvoiceItemRequest> items) {}
