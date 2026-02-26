package com.wallet.common.dto;

import java.math.BigDecimal;

public record QrDecodeResponseDTO(
        String targetWalletId,
        BigDecimal amount,
        String status
) {
}
