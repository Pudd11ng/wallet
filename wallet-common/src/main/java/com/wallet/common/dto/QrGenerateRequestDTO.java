package com.wallet.common.dto;

import com.wallet.common.enums.QrFormat;

import java.math.BigDecimal;

public record QrGenerateRequestDTO(
        BigDecimal amount,
        QrFormat format
) {
}
