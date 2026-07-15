package com.pm.billingservice.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record PaymentRequest(
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 40) String paymentMethod,
        @Size(max = 120) String reference) {}
