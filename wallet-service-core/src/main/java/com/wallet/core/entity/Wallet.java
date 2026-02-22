package com.wallet.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Wallet(
        String id,
        String userId,
        BigDecimal balance,
        String currency,
        String status,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
