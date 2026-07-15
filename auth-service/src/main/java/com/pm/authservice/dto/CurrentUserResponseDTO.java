package com.pm.authservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The currently authenticated staff user")
public record CurrentUserResponseDTO(
        @Schema(example = "1") Long userId,
        @Schema(example = "admin@pm.com") String email,
        @Schema(example = "ADMIN") String role
) {
}
