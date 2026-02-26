package com.wallet.common.dto;

import java.math.BigDecimal;

public record TransferEventDTO(
        String transactionId,
        String fromWalletId,
        String toWalletId,
        BigDecimal amount,
        String status
) {}