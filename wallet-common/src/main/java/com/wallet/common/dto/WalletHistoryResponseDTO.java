package com.wallet.common.dto;

import java.math.BigDecimal;
import java.util.List;

public record WalletHistoryResponseDTO(
        String walletId,
        BigDecimal currentBalance,
        String currency,
        List<TransactionHistoryDTO> transactions
) {
}
