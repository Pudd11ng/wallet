package com.wallet.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record JournalEntry(
        Long id,
        String transactionId,
        String walletId,
        String type,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
