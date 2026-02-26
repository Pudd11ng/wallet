package com.wallet.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TopUpRequestDTO(
        @NotBlank(message = "Wallet ID is required")
        String walletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Minimum top-up amount is 1.00")
        BigDecimal amount,

        @NotBlank(message = "Source is required (e.g., BANK_FPX)")
        String source,

        @NotBlank(message = "Reference ID is required")
        String referenceId
) {
}
