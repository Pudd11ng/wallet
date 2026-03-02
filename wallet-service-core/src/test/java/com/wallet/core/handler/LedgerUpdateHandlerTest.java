package com.wallet.core.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.dto.TransferEventDTO;
import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.JournalEntry;
import com.wallet.core.entity.OutboxEvent;
import com.wallet.core.entity.TransactionRequest;
import com.wallet.core.entity.Wallet;
import com.wallet.core.mapper.WalletMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerUpdateHandlerTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private LedgerUpdateHandler ledgerUpdateHandler;

    @Test
    void process_WhenRequestIsValid_CompletesSuccessfully() throws JsonProcessingException {
        // Arrange
        Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        Wallet receiver = new Wallet("w2", "user2", new BigDecimal("50.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        TransferRequestDTO requestDTO = new TransferRequestDTO(sender.id(), receiver.id(), new BigDecimal("10.00"),
                "Test");

        TransactionContext context = TransactionContext.builder()
                .transactionId("TXN-123")
                .requestId("REQ-123")
                .request(requestDTO)
                .senderWallet(sender)
                .receiverWallet(receiver)
                .build();

        when(walletMapper.updateWalletBalance(eq(sender.id()), eq(new BigDecimal("90.00")), eq(1))).thenReturn(1);
        when(walletMapper.updateWalletBalance(eq(receiver.id()), eq(new BigDecimal("60.00")), eq(1))).thenReturn(1);
        when(objectMapper.writeValueAsString(any(TransferEventDTO.class))).thenReturn("{\"event\":\"test\"}");

        // Act & Assert
        assertThatCode(() -> ledgerUpdateHandler.process(context)).doesNotThrowAnyException();

        verify(walletMapper, times(2)).updateWalletBalance(anyString(), any(BigDecimal.class), anyInt());
        verify(walletMapper, times(2)).insertJournalEntry(any(JournalEntry.class));
        verify(walletMapper, times(1)).insertTransactionRequest(any(TransactionRequest.class));
        verify(walletMapper, times(1)).insertOutboxEvent(any(OutboxEvent.class));
    }

    @Test
    void process_WhenSenderOptimisticLockFails_ThrowsException() {
        // Arrange
        Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        Wallet receiver = new Wallet("w2", "user2", new BigDecimal("50.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        TransferRequestDTO requestDTO = new TransferRequestDTO(sender.id(), receiver.id(), new BigDecimal("10.00"),
                "Test");

        TransactionContext context = TransactionContext.builder()
                .transactionId("TXN-123")
                .requestId("REQ-123")
                .request(requestDTO)
                .senderWallet(sender)
                .receiverWallet(receiver)
                .build();

        when(walletMapper.updateWalletBalance(eq(sender.id()), eq(new BigDecimal("90.00")), eq(1))).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> ledgerUpdateHandler.process(context))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Concurrency error: Sender wallet state changed.");

        verify(walletMapper, never()).insertJournalEntry(any());
    }

    @Test
    void process_WhenReceiverOptimisticLockFails_ThrowsException() {
        // Arrange
        Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        Wallet receiver = new Wallet("w2", "user2", new BigDecimal("50.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        TransferRequestDTO requestDTO = new TransferRequestDTO(sender.id(), receiver.id(), new BigDecimal("10.00"),
                "Test");

        TransactionContext context = TransactionContext.builder()
                .transactionId("TXN-123")
                .requestId("REQ-123")
                .request(requestDTO)
                .senderWallet(sender)
                .receiverWallet(receiver)
                .build();

        when(walletMapper.updateWalletBalance(eq(sender.id()), eq(new BigDecimal("90.00")), eq(1))).thenReturn(1);
        when(walletMapper.updateWalletBalance(eq(receiver.id()), eq(new BigDecimal("60.00")), eq(1))).thenReturn(0);

        // Act & Assert
        assertThatThrownBy(() -> ledgerUpdateHandler.process(context))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Concurrency error: Receiver wallet state changed.");
    }

    @Test
    void process_WhenJsonProcessingExceptionOccurs_ThrowsWalletBusinessException() throws JsonProcessingException {
        // Arrange
        Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        Wallet receiver = new Wallet("w2", "user2", new BigDecimal("50.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                LocalDateTime.now());
        TransferRequestDTO requestDTO = new TransferRequestDTO(sender.id(), receiver.id(), new BigDecimal("10.00"),
                "Test");

        TransactionContext context = TransactionContext.builder()
                .transactionId("TXN-123")
                .requestId("REQ-123")
                .request(requestDTO)
                .senderWallet(sender)
                .receiverWallet(receiver)
                .build();

        when(walletMapper.updateWalletBalance(eq(sender.id()), eq(new BigDecimal("90.00")), eq(1))).thenReturn(1);
        when(walletMapper.updateWalletBalance(eq(receiver.id()), eq(new BigDecimal("60.00")), eq(1))).thenReturn(1);

        JsonProcessingException exception = mock(JsonProcessingException.class);
        when(objectMapper.writeValueAsString(any(TransferEventDTO.class))).thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> ledgerUpdateHandler.process(context))
                .isInstanceOf(WalletBusinessException.class)
                .hasMessage("Internal Error: Failed to format outbox event");

        verify(walletMapper, times(1)).insertTransactionRequest(any(TransactionRequest.class));
        verify(walletMapper, never()).insertOutboxEvent(any());
    }
}
