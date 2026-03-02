package com.wallet.core.service;

import com.wallet.common.dto.InitializeWalletRequestDTO;
import com.wallet.common.dto.InitializeWalletResponseDTO;
import com.wallet.common.dto.TransactionHistoryDTO;
import com.wallet.common.dto.WalletHistoryResponseDTO;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.JournalEntry;
import com.wallet.core.entity.Wallet;
import com.wallet.core.mapper.WalletMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletManagementServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletManagementService walletManagementService;

    @Test
    void initializeWallet_WhenValidRequest_SavesWalletAndReturnsResponse() {
        // Arrange
        String userId = "user-123";
        String clientId = "user-123";
        String currency = "USD";
        InitializeWalletRequestDTO request = new InitializeWalletRequestDTO(userId, currency);

        when(walletMapper.findWalletByUserId(userId)).thenReturn(Optional.empty());

        // Act
        InitializeWalletResponseDTO response = walletManagementService.initializeWallet(request, clientId);

        // Assert
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletMapper).insertWallet(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();
        assertThat(savedWallet.id()).startsWith("W-");
        assertThat(savedWallet.userId()).isEqualTo(userId);
        assertThat(savedWallet.balance()).isEqualTo(BigDecimal.ZERO.setScale(4));
        assertThat(savedWallet.currency()).isEqualTo(currency);
        assertThat(savedWallet.status()).isEqualTo("ACTIVE");

        assertThat(response.walletId()).isEqualTo(savedWallet.id());
        assertThat(response.balance()).isEqualTo(savedWallet.balance());
        assertThat(response.currency()).isEqualTo(currency);
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void getWalletIdByUserId_WhenWalletExists_ReturnsWalletId() {
        // Arrange
        String userId = "user-123";
        String expectedWalletId = "W-ABCDEF";
        Wallet wallet = new Wallet(expectedWalletId, userId, BigDecimal.ZERO, "USD", "ACTIVE", 0, LocalDateTime.now(),
                LocalDateTime.now());

        when(walletMapper.findWalletByUserId(userId)).thenReturn(Optional.of(wallet));

        // Act
        String result = walletManagementService.getWalletIdByUserId(userId);

        // Assert
        assertThat(result).isEqualTo(expectedWalletId);
    }

    @Test
    void getWalletIdByUserId_WhenWalletNotFound_ThrowsException() {
        // Arrange
        String userId = "user-123";

        when(walletMapper.findWalletByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> walletManagementService.getWalletIdByUserId(userId))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Wallet not found for user: " + userId);
    }

    @Test
    void getWalletHistory_WhenCalled_ReturnsMappedHistory() {
        // Arrange
        String walletId = "W-123456";
        String clientId = "user-123";
        Wallet wallet = new Wallet(walletId, clientId, new BigDecimal("100.00"), "USD", "ACTIVE", 1,
                LocalDateTime.now(), LocalDateTime.now());

        LocalDateTime txTime = LocalDateTime.now().minusDays(1);
        JournalEntry entry = new JournalEntry(1L, "TXN-111", walletId, "CREDIT", new BigDecimal("50.00"), txTime);

        when(walletMapper.findWalletById(walletId)).thenReturn(Optional.of(wallet));
        when(walletMapper.findJournalEntriesByWalletId(walletId)).thenReturn(List.of(entry));

        // Act
        WalletHistoryResponseDTO response = walletManagementService.getWalletHistory(walletId, clientId);

        // Assert
        assertThat(response.walletId()).isEqualTo(walletId);
        assertThat(response.currentBalance()).isEqualTo(new BigDecimal("100.00"));
        assertThat(response.currency()).isEqualTo("USD");

        List<TransactionHistoryDTO> historyDto = response.transactions();
        assertThat(historyDto).hasSize(1);

        TransactionHistoryDTO mappedDto = historyDto.get(0);
        assertThat(mappedDto.transactionId()).isEqualTo("TXN-111");
        assertThat(mappedDto.type()).isEqualTo("CREDIT");
        assertThat(mappedDto.amount()).isEqualTo(new BigDecimal("50.00"));
        assertThat(mappedDto.timestamp()).isEqualTo(txTime);
    }
}
