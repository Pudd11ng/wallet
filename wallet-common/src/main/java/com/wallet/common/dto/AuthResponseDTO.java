package com.wallet.common.dto;

public record AuthResponseDTO(
        String userId,
        String token,
        String message
) {
}
