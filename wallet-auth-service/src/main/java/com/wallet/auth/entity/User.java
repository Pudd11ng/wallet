package com.wallet.auth.entity;

import java.time.LocalDateTime;

public record User(
        String id,
        String username,
        String passwordHash,
        LocalDateTime createdAt
) {
}
