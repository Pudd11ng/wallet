package com.wallet.core.strategy;

import com.wallet.common.dto.QrDecodeResponseDTO;
import com.wallet.common.exception.WalletBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class InternalHmacQrStrategyTest {

    @InjectMocks
    private InternalHmacQrStrategy qrStrategy;

    private final String dummySecret = "TestSecretKey123!@#";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(qrStrategy, "qrSecret", dummySecret);
    }

    @Test
    void generate_WhenValidInput_ReturnsProperlyFormattedString() {
        // Arrange
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("50.00");

        // Act
        String qrString = qrStrategy.generate(walletId, amount);

        // Assert
        assertThat(qrString).isNotNull();
        String[] parts = qrString.split("\\|");
        assertThat(parts).hasSize(4);
        assertThat(parts[0]).isEqualTo(walletId);
        assertThat(parts[1]).isEqualTo("50.00");
        assertThat(Long.parseLong(parts[2])).isGreaterThan(Instant.now().getEpochSecond());
        assertThat(parts[3]).isNotBlank(); // Signature
    }

    @Test
    void decodeAndVerify_WhenInvalidFormat_ThrowsException() {
        // Arrange
        String invalidQrString = "wallet-123|50.00|1999999999"; // Missing signature

        // Act & Assert
        assertThatThrownBy(() -> qrStrategy.decodeAndVerify(invalidQrString))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Invalid QR Code format");
    }

    @Test
    void decodeAndVerify_WhenTamperedSignature_ThrowsException() {
        // Arrange
        String validQrString = qrStrategy.generate("wallet-123", new BigDecimal("50.00"));
        String[] parts = validQrString.split("\\|");
        // Tamper with the amount
        String tamperedQrString = parts[0] + "|500.00|" + parts[2] + "|" + parts[3];

        // Act & Assert
        assertThatThrownBy(() -> qrStrategy.decodeAndVerify(tamperedQrString))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("SECURITY ALERT: QR Code signature is invalid or tampered with!");
    }

    @Test
    void decodeAndVerify_WhenExpired_ThrowsException() {
        // Arrange
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("50.00");
        long pastExpiration = Instant.now().getEpochSecond() - 100; // Expired 100 seconds ago
        String payload = walletId + "|" + amount.toString() + "|" + pastExpiration;

        // We need to generate a valid signature for an expired payload to pass the
        // signature check
        String expiredQrString = "";
        try {
            java.lang.reflect.Method method = InternalHmacQrStrategy.class.getDeclaredMethod("generateHmac",
                    String.class);
            method.setAccessible(true);
            String signature = (String) method.invoke(qrStrategy, payload);
            expiredQrString = payload + "|" + signature;
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("Failed to setup expired QR string");
        }

        System.out.println(expiredQrString);

        // Act & Assert
        final String finalExpiredQrString = expiredQrString;
        assertThatThrownBy(() -> qrStrategy.decodeAndVerify(finalExpiredQrString))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("This QR Code has expired.");
    }

    @Test
    void decodeAndVerify_WhenValidAndAuthentic_ReturnsSuccessResponse() {
        // Arrange
        String walletId = "wallet-123";
        BigDecimal amount = new BigDecimal("50.00");
        String validQrString = qrStrategy.generate(walletId, amount);

        // Act
        QrDecodeResponseDTO response = qrStrategy.decodeAndVerify(validQrString);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.targetWalletId()).isEqualTo(walletId);
        assertThat(response.amount()).isEqualTo(amount);
        assertThat(response.status()).isEqualTo("VALID_AND_VERIFIED");
    }
}
