package com.wallet.core.strategy;

import com.wallet.common.dto.QrDecodeResponseDTO;
import com.wallet.common.enums.QrFormat;
import java.math.BigDecimal;

public interface QrProcessingStrategy {
    // Identifies which strategy this is
    QrFormat getSupportedFormat();

    // Creates the secure string to be drawn by the mobile app
    String generate(String walletId, BigDecimal amount);

    // Reads a scanned string and verifies the cryptography
    QrDecodeResponseDTO decodeAndVerify(String qrString);
}
