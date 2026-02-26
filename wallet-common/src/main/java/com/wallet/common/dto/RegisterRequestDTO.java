package com.wallet.common.dto;

import jakarta.validation.constraints.NotNull;

public record RegisterRequestDTO(
        @NotNull(message = "Username is required")
        String username,

        @NotNull(message = "Password is required")
        String password
) {
}
