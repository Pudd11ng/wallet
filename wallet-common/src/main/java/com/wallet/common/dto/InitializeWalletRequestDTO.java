package com.wallet.common.dto;
import jakarta.validation.constraints.NotBlank;

public record InitializeWalletRequestDTO(
        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "Currency is required")
        String currency
) {
}
