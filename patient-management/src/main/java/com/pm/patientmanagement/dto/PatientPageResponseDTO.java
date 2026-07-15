package com.pm.patientmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Paginated patient search result")
public record PatientPageResponseDTO(
        List<PatientResponseDTO> patients,
        int currentPage,
        int pageSize,
        long totalElements,
        int totalPages
) {
}
