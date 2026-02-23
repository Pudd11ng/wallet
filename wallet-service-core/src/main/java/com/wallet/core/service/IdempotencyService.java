package com.wallet.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.wallet.common.exception.WalletBusinessException;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public void checkAndLock(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("X-Request-ID header is missing");
        }

        String redisKey = "IDEMPOTENCY:" + requestId;

        // Attempts to save the key with a 24-hour expiration.
        // Returns true if the key was saved, false if it already existed.
        Boolean isNewRequest = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "LOCKED", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNewRequest)) {
            log.warn("Duplicate request detected and blocked by Redis: {}", requestId);
            throw new WalletBusinessException("Transaction already processed with Request ID: " + requestId);
            // Note: We will map this to a proper Custom Exception in Phase B!
        }

        log.info("Request ID {} locked in Redis for 24 hours", requestId);
    }
}