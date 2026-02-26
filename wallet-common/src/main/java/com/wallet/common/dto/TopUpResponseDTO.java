package com.wallet.common.dto;

import java.math.BigDecimal;

public record TopUpResponseDTO(
        String transactionId,
        BigDecimal newBalance,
        String currency
) {
}
