package com.wallet.common.dto;

import com.wallet.common.enums.QrFormat;

public record QrDecodeRequestDTO(
        String qrString,
        QrFormat format
) {
}
