package com.wallet.common.dto;

import java.math.BigDecimal;

public record InitializeWalletResponseDTO(
        String walletId,
        BigDecimal balance,
        String currency,
        String status
) {
}
