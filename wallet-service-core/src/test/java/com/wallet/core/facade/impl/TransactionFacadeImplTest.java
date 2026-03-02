package com.wallet.core.facade.impl;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.dto.WalletResponseDTO;
import com.wallet.core.entity.Wallet;
import com.wallet.core.facade.AuthFacade;
import com.wallet.core.handler.TransactionContext;
import com.wallet.core.handler.TransactionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionFacadeImplTest {

    @Mock
    private AuthFacade authFacade;

    @Mock
    private TransactionHandler mockHandler1;

    @Mock
    private TransactionHandler mockHandler2;

    private TransactionFacadeImpl transactionFacade;

    @BeforeEach
    void setUp() {
        // Arrange manual handler chain injection
        List<TransactionHandler> handlerChain = List.of(mockHandler1, mockHandler2);
        transactionFacade = new TransactionFacadeImpl(handlerChain, authFacade);
    }

    @Test
    void executeTransfer_WhenRequestIsValid_OrchestratesHandlersAndCalculatesBalanceSuccessfully() {
        // Arrange
        String requestId = "REQ-123";
        String clientId = "client-456";
        TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w2", new BigDecimal("15.50"), "Test");

        when(authFacade.fetchUsername(clientId)).thenReturn("john_doe");

        // Simulate ValidationHandler behavior: it sets the sender wallet in the context
        doAnswer(invocation -> {
            TransactionContext context = invocation.getArgument(0);
            Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1, LocalDateTime.now(),
                    LocalDateTime.now());
            context.setSenderWallet(sender);
            return null;
        }).when(mockHandler1).process(any(TransactionContext.class));

        // Act
        WalletResponseDTO response = transactionFacade.executeTransfer(requestId, clientId, requestDTO);

        // Assert
        // 1. Verify AuthFacade was called
        verify(authFacade, times(1)).fetchUsername(clientId);

        // 2. Verify Handlers were called in order with the same context
        ArgumentCaptor<TransactionContext> contextCaptor = ArgumentCaptor.forClass(TransactionContext.class);
        verify(mockHandler1, times(1)).process(contextCaptor.capture());
        verify(mockHandler2, times(1)).process(contextCaptor.capture());

        // Ensure context was built correctly and passed to handlers
        TransactionContext capturedContext = contextCaptor.getAllValues().get(0);
        assertThat(capturedContext.getRequestId()).isEqualTo(requestId);
        assertThat(capturedContext.getClientId()).isEqualTo(clientId);
        assertThat(capturedContext.getSenderUsername()).isEqualTo("john_doe");
        assertThat(capturedContext.getTransactionId()).startsWith("TXN-");
        assertThat(capturedContext.getRequest()).isEqualTo(requestDTO);

        // 3. Verify final balance calculation
        // Sender balance was 100.00, amount was 15.50 -> expected 84.50
        assertThat(response.walletId()).isEqualTo("w1");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.currentBalance()).isEqualTo(new BigDecimal("84.50"));
    }
}
