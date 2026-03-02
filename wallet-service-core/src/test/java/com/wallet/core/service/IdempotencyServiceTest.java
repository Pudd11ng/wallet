package com.wallet.core.service;

import com.wallet.common.exception.WalletBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        // Essential setup: the service calls redisTemplate.opsForValue().setIfAbsent(...)
        // We must mock the chained method call to prevent NullPointerExceptions.
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isRequestProcessed_WhenKeyIsAbsent_SetsLockAndReturnsFalse() {
        // Arrange
        String requestId = "REQ-123";
        String redisKey = "IDEMPOTENCY:" + requestId;

        // Mock Redis to simulate the key does NOT exist, so it successfully sets it
        // (returns true)
        when(valueOperations.setIfAbsent(eq(redisKey), eq("LOCKED"), any(Duration.class))).thenReturn(true);

        // Act & Assert
        assertThatCode(() -> idempotencyService.checkAndLock(requestId))
                .doesNotThrowAnyException();

        verify(valueOperations, times(1)).setIfAbsent(eq(redisKey), eq("LOCKED"), eq(Duration.ofHours(24)));
    }

    @Test
    void isRequestProcessed_WhenKeyExists_ReturnsTrue() {
        // Arrange
        String requestId = "REQ-456";
        String redisKey = "IDEMPOTENCY:" + requestId;

        // Mock Redis to simulate the key ALREADY exists, so it fails to set it (returns
        // false)
        when(valueOperations.setIfAbsent(eq(redisKey), eq("LOCKED"), any(Duration.class))).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> idempotencyService.checkAndLock(requestId))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Transaction already processed with Request ID: " + requestId);
    }

    @Test
    void isRequestProcessed_WhenRequestIdIsNull_ThrowsIllegalArgumentException() {
        // Act & Assert
        assertThatThrownBy(() -> idempotencyService.checkAndLock(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("X-Request-ID header is missing");

        // Ensure Redis is never called if validation fails upfront.
        verify(redisTemplate, never()).opsForValue();
    }
}
