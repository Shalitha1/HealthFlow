package com.pm.patientmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Patient entity - represents a patient in the system
 */
@Entity
@Table(name = "patients", indexes = {
        @Index(name = "idx_email", columnList = "email", unique = true),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    /**
     * Primary key - auto-incremented long (not UUID for better performance)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Patient name
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * Patient email - unique
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    /**
     * Patient address
     */
    @Column(length = 500)
    private String address;

    /**
     * Date of birth
     */
    @Column(nullable = false)
    private LocalDate dateOfBirth;

    /**
     * Registration date - auto-populated
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    @Column
    private LocalDateTime updatedAt;

    /**
     * Is patient active?
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Auto-populate timestamps and active flag
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}