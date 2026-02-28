package com.wallet.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionHistoryDTO(
        String transactionId,
        String type, // CREDIT or DEBIT
        BigDecimal amount,
        LocalDateTime timestamp
) {
    public static TransactionHistoryDTO create(String txId, String type, BigDecimal amount, LocalDateTime time) {
        return new TransactionHistoryDTO(txId, type, amount, time);
    }
}