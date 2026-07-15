package com.pm.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequestDTO(
        @NotBlank(message = "Token is required")
        String token
) {
    @Override
    public String toString() {
        return "ValidateTokenRequestDTO[token=[REDACTED]]";
    }
}
