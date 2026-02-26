package com.wallet.common.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String errorCode,
        String errorMessage,
        String requestId,
        LocalDateTime timestamp
) {}