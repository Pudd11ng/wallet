package com.wallet.core.strategy;

import com.wallet.common.dto.QrDecodeResponseDTO;
import com.wallet.common.enums.QrFormat;
import com.wallet.common.exception.WalletBusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DuitNowTlvQrStrategyTest {

    @InjectMocks
    private DuitNowTlvQrStrategy strategy;

    @Test
    void getSupportedFormat_ReturnsDuitNowTlv() {
        assertThat(strategy.getSupportedFormat()).isEqualTo(QrFormat.DUITNOW_TLV);
    }

    @Test
    void generate_WhenValidInput_ReturnsEmvcoFormattedStringWithDynamicCrc() {
        // Act
        String qrData = strategy.generate("RECEIVER-123", new BigDecimal("100.50"));

        // Assert
        assertThat(qrData).startsWith("000201");
        assertThat(qrData).contains("010211"); // Tag 01, Len 02, Val 11
        assertThat(qrData).contains("RECEIVER-123");
        assertThat(qrData).contains("5406100.50"); // Tag 54, Len 06, Val 100.50
        assertThat(qrData.matches(".*6304[0-9A-F]{4}$")).isTrue();
    }

    @Test
    void decodeAndVerify_WhenValidDynamicQr_ReturnsParsedResponse() {
        // Arrange: Generate a perfectly valid, mathematically signed EMVCo string
        String validQr = strategy.generate("MERCHANT-DUITNOW-999", new BigDecimal("50.00"));

        // Act: Pass it to our dynamic decoder
        QrDecodeResponseDTO response = strategy.decodeAndVerify(validQr);

        // Assert: It should perfectly extract the nested Merchant ID and Amount
        assertThat(response.status()).isEqualTo("DUITNOW_VERIFIED");
        assertThat(response.targetWalletId()).isEqualTo("MERCHANT-DUITNOW-999");
        assertThat(response.amount()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void decodeAndVerify_WhenTamperedAmount_ThrowsCrcException() {
        // Arrange: Generate a valid string
        String validQr = strategy.generate("MERCHANT", new BigDecimal("10.00"));

        // Simulate a hacker changing "10.00" (Len 05) to "99.99" (Len 05) in transit
        String tamperedQr = validQr.replace("540510.00", "540599.99");

        // Act & Assert: The CRC polynomial math should catch the difference
        assertThatThrownBy(() -> strategy.decodeAndVerify(tamperedQr))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessageContaining("SECURITY ALERT: Invalid QR signature (CRC failed)");
    }

    @Test
    void decodeAndVerify_WhenInvalidHeader_ThrowsException() {
        // Arrange
        String invalidQr = "INVALID-HEADER-123";

        // Act & Assert
        assertThatThrownBy(() -> strategy.decodeAndVerify(invalidQr))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessageContaining("Invalid DuitNow EMVCo TLV format");
    }
}