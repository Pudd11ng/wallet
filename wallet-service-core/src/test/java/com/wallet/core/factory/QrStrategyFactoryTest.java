package com.wallet.core.factory;

import com.wallet.common.enums.QrFormat;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.strategy.InternalHmacQrStrategy;
import com.wallet.core.strategy.QrProcessingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrStrategyFactoryTest {

    private QrStrategyFactory qrStrategyFactory;
    private InternalHmacQrStrategy internalStrategy;

    @BeforeEach
    void setUp() {
        // Arrange manual instantation with dummy strategy
        internalStrategy = new InternalHmacQrStrategy();
        qrStrategyFactory = new QrStrategyFactory(List.of(internalStrategy));
    }

    @Test
    void getStrategy_WhenFormatSupported_ReturnsStrategy() {
        // Act
        QrProcessingStrategy result = qrStrategyFactory.getStrategy(QrFormat.INTERNAL_HMAC);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(InternalHmacQrStrategy.class);
        assertThat(result.getSupportedFormat()).isEqualTo(QrFormat.INTERNAL_HMAC);
    }

    @Test
    void getStrategy_WhenFormatUnsupported_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> qrStrategyFactory.getStrategy(QrFormat.DUITNOW_TLV))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Unsupported QR Format: " + QrFormat.DUITNOW_TLV);
    }

    @Test
    void getStrategy_WhenFormatIsNull_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> qrStrategyFactory.getStrategy(null))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Unsupported QR Format: null");
    }
}
