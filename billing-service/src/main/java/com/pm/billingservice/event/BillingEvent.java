package com.pm.billingservice.event;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
public record BillingEvent(String eventType, String billingAccountId, Long invoiceId, Long paymentId,
        String patientId, BigDecimal amount, String status, String actorId, OffsetDateTime occurredAt) {}
