package com.pm.auditservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private String actorId;

    private LocalDateTime timestamp;

    protected AuditLog() {
    }

    public AuditLog(String eventType, String payload, String actorId, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.payload = payload;
        this.actorId = actorId;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getActorId() {
        return actorId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}