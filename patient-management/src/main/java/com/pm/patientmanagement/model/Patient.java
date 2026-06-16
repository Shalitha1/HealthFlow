package com.pm.patientmanagement.model;


import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id;
    private String name;
    private String email;
    private String address;
    private LocalDate dateOfBirth;
    @Column(nullable = false)
    private LocalDate registeredDate;
}
