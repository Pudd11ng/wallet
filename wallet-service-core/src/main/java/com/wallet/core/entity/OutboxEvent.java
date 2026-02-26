package com.wallet.core.entity;

import java.time.LocalDateTime;

public record OutboxEvent(
        Long id,
        String topic,
        String payload,
        String status,
        LocalDateTime createdAt
) {
}