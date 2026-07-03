package com.pm.billingservice.dto;

public record CreateBillingRequest(
        String patientId,
        String name,
        String email
) {
}
