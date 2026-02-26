package com.wallet.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TransferRequestDTO(
        @NotBlank(message = "Sender Wallet ID is required")
        String fromWalletId,

        @NotBlank(message = "Receiver Wallet ID is required")
        String toWalletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
        BigDecimal amount,

        String remark
) {}