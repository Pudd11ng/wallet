package com.wallet.core.handler;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.Wallet;
import com.wallet.core.mapper.WalletMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationHandlerTest {

        @Mock
        private WalletMapper walletMapper;

        @InjectMocks
        private ValidationHandler validationHandler;

        @Test
        void process_WhenAmountIsLessThanOrEqualZero_ThrowsException() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w2", BigDecimal.ZERO, "Test");
                TransactionContext context = TransactionContext.builder()
                                .requestId("REQ-123")
                                .request(requestDTO)
                                .build();

                // Act & Assert
                assertThatThrownBy(() -> validationHandler.process(context))
                                .isInstanceOf(WalletBusinessException.class)
                                .hasMessage("Transfer amount must be greater than zero");
        }

        @Test
        void process_WhenSameWalletIsProvided_ThrowsException() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w1", new BigDecimal("10.00"), "Test");
                TransactionContext context = TransactionContext.builder()
                                .requestId("REQ-123")
                                .request(requestDTO)
                                .build();

                // Act & Assert
                assertThatThrownBy(() -> validationHandler.process(context))
                                .isInstanceOf(WalletBusinessException.class)
                                .hasMessage("Cannot transfer funds to the same wallet");
        }

        @Test
        void process_WhenSenderIsNotFound_ThrowsException() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w2", new BigDecimal("10.00"), "Test");
                TransactionContext context = TransactionContext.builder()
                                .requestId("REQ-123")
                                .request(requestDTO)
                                .build();

                when(walletMapper.findWalletById("w1")).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> validationHandler.process(context))
                                .isInstanceOf(WalletBusinessException.class)
                                .hasMessage("Sender wallet not found");
        }

        @Test
        void process_WhenUserIsUnauthorized_ThrowsException() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w2", new BigDecimal("10.00"), "Test");
                TransactionContext context = TransactionContext.builder()
                                .requestId("REQ-123")
                                .clientId("user2") // Wrong user
                                .request(requestDTO)
                                .build();

                Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1,
                                LocalDateTime.now(), LocalDateTime.now());
                when(walletMapper.findWalletById("w1")).thenReturn(Optional.of(sender));

                // Act & Assert
                assertThatThrownBy(() -> validationHandler.process(context))
                                .isInstanceOf(WalletBusinessException.class)
                                .hasMessage("Unauthorized: You cannot transfer funds from a wallet you do not own.");
        }

        @Test
        void process_WhenReceiverIsNotFound_ThrowsException() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w2", new BigDecimal("10.00"), "Test");
                TransactionContext context = TransactionContext.builder()
                                .requestId("REQ-123")
                                .clientId("user1")
                                .request(requestDTO)
                                .build();

                Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1,
                                LocalDateTime.now(), LocalDateTime.now());
                when(walletMapper.findWalletById("w1")).thenReturn(Optional.of(sender));
                when(walletMapper.findWalletById("w2")).thenReturn(Optional.empty());

                // Act & Assert
                assertThatThrownBy(() -> validationHandler.process(context))
                                .isInstanceOf(WalletBusinessException.class)
                                .hasMessage("Receiver wallet not found");
        }

        @Test
        void process_WhenFundsAreInsufficient_ThrowsException() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w2", new BigDecimal("150.00"), "Test"); // Exceeds
                                                                                                                      // balance
                TransactionContext context = TransactionContext.builder()
                                .requestId("REQ-123")
                                .clientId("user1")
                                .request(requestDTO)
                                .build();

                Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1,
                                LocalDateTime.now(), LocalDateTime.now());
                Wallet receiver = new Wallet("w2", "user2", new BigDecimal("50.00"), "USD", "ACTIVE", 1,
                                LocalDateTime.now(), LocalDateTime.now());

                when(walletMapper.findWalletById("w1")).thenReturn(Optional.of(sender));
                when(walletMapper.findWalletById("w2")).thenReturn(Optional.of(receiver));

                // Act & Assert
                assertThatThrownBy(() -> validationHandler.process(context))
                                .isInstanceOf(WalletBusinessException.class)
                                .hasMessage("Insufficient funds");
        }

        @Test
        void process_WhenRequestIsValid_CompletesSuccessfully() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("w1", "w2", new BigDecimal("10.00"), "Test");
                TransactionContext context = TransactionContext.builder()
                                .requestId("REQ-123")
                                .clientId("user1")
                                .request(requestDTO)
                                .build();

                Wallet sender = new Wallet("w1", "user1", new BigDecimal("100.00"), "USD", "ACTIVE", 1,
                                LocalDateTime.now(), LocalDateTime.now());
                Wallet receiver = new Wallet("w2", "user2", new BigDecimal("50.00"), "USD", "ACTIVE", 1,
                                LocalDateTime.now(), LocalDateTime.now());

                when(walletMapper.findWalletById("w1")).thenReturn(Optional.of(sender));
                when(walletMapper.findWalletById("w2")).thenReturn(Optional.of(receiver));

                // Act
                validationHandler.process(context);

                // Assert
                assertThat(context.getSenderWallet()).isEqualTo(sender);
                assertThat(context.getReceiverWallet()).isEqualTo(receiver);
        }
}
