package com.pm.billingservice.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record InvoiceItemRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @Min(1) Integer quantity,
        @NotNull @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal unitPrice) {}
