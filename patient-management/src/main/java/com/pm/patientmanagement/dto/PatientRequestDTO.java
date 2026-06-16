package com.pm.patientmanagement.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRequestDTO {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Address is required.")
    private String address;

    @NotNull(message = "Date of birth is required.")
    @Past(message = "Date of birth must be in past")
    private LocalDate dateOfBirth;
}
