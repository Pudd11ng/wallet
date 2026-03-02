package com.wallet.core.service;

import com.wallet.common.dto.QrDecodeRequestDTO;
import com.wallet.common.dto.QrDecodeResponseDTO;
import com.wallet.common.dto.QrGenerateRequestDTO;
import com.wallet.common.enums.QrFormat;
import com.wallet.core.factory.QrStrategyFactory;
import com.wallet.core.strategy.QrProcessingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrServiceTest {

    @Mock
    private QrStrategyFactory qrStrategyFactory;

    @Mock
    private WalletManagementService walletManagementService;

    @Mock
    private QrProcessingStrategy mockStrategy;

    @InjectMocks
    private QrService qrService;

    @Test
    void generateQrCode_WhenCalled_OrchestratesCorrectly() {
        // Arrange
        String clientId = "user-123";
        String walletId = "wallet-456";
        BigDecimal amount = new BigDecimal("100.00");
        QrFormat format = QrFormat.INTERNAL_HMAC;
        QrGenerateRequestDTO request = new QrGenerateRequestDTO(amount, format);
        String expectedQrString = "wallet-456|100.00|1234567890|signature";

        when(walletManagementService.getWalletIdByUserId(clientId)).thenReturn(walletId);
        when(qrStrategyFactory.getStrategy(format)).thenReturn(mockStrategy);
        when(mockStrategy.generate(walletId, amount)).thenReturn(expectedQrString);

        // Act
        String result = qrService.generateQrCode(clientId, request);

        // Assert
        assertThat(result).isEqualTo(expectedQrString);
        verify(walletManagementService, times(1)).getWalletIdByUserId(clientId);
        verify(qrStrategyFactory, times(1)).getStrategy(format);
        verify(mockStrategy, times(1)).generate(walletId, amount);
    }

    @Test
    void decodeQrCode_WhenCalled_OrchestratesCorrectly() {
        // Arrange
        String qrString = "wallet-456|100.00|1234567890|signature";
        QrFormat format = QrFormat.INTERNAL_HMAC;
        QrDecodeRequestDTO request = new QrDecodeRequestDTO(qrString, format);
        QrDecodeResponseDTO expectedResponse = new QrDecodeResponseDTO("wallet-456", new BigDecimal("100.00"),
                "VALID_AND_VERIFIED");

        when(qrStrategyFactory.getStrategy(format)).thenReturn(mockStrategy);
        when(mockStrategy.decodeAndVerify(qrString)).thenReturn(expectedResponse);

        // Act
        QrDecodeResponseDTO result = qrService.decodeQrCode(request);

        // Assert
        assertThat(result).isEqualTo(expectedResponse);
        verify(qrStrategyFactory, times(1)).getStrategy(format);
        verify(mockStrategy, times(1)).decodeAndVerify(qrString);
    }
}
