package com.wallet.common.dto;

import java.math.BigDecimal;

public record WalletResponseDTO(
        String walletId,
        BigDecimal currentBalance,
        String currency,
        String status
) {}
