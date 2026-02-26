package com.wallet.core.entity;

import java.math.BigDecimal;
import  java.time.LocalDateTime;

public record TransactionRequest(
        String id,
        String requestId,
        String type,
        String status,
        BigDecimal amount,
        LocalDateTime createdAt
) {
}
