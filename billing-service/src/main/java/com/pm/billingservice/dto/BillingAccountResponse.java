package com.pm.billingservice.dto;
import com.pm.billingservice.model.BillingAccount;
import java.time.OffsetDateTime;
public record BillingAccountResponse(String id, String patientId, String status, OffsetDateTime createdAt) {
    public static BillingAccountResponse from(BillingAccount value) { return new BillingAccountResponse(value.getId(), value.getPatientId(), value.getStatus().name(), value.getCreatedAt()); }
}
