package com.pm.billingservice.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "billing_accounts")
public class BillingAccount {
    @Id private String id;
    @Column(name = "patient_id", nullable = false, unique = true) private String patientId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BillingAccountStatus status;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    protected BillingAccount() {}
    public BillingAccount(String patientId) {
        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.status = BillingAccountStatus.ACTIVE;
        this.createdAt = OffsetDateTime.now();
    }
    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public BillingAccountStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
