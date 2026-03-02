package com.wallet.core.handler;

import com.wallet.common.dto.TransferRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LimitCheckHandlerTest {

        @InjectMocks
        private LimitCheckHandler limitCheckHandler;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(limitCheckHandler, "maxTransferLimit", new BigDecimal("10000.00"));
        }

        @Test
        void process_WhenAmountIsLessThanLimit_Passes() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("wallet1", "wallet2", new BigDecimal("5000.00"),
                                "Lunch");
                TransactionContext context = TransactionContext.builder()
                                .transactionId("TXN-12345")
                                .request(requestDTO)
                                .build();

                // Act & Assert
                assertThatCode(() -> limitCheckHandler.process(context))
                                .doesNotThrowAnyException();
        }

        @Test
        void process_WhenAmountEqualsLimit_Passes() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("wallet1", "wallet2", new BigDecimal("10000.00"),
                                "Rent");
                TransactionContext context = TransactionContext.builder()
                                .transactionId("TXN-12345")
                                .request(requestDTO)
                                .build();

                // Act & Assert
                assertThatCode(() -> limitCheckHandler.process(context))
                                .doesNotThrowAnyException();
        }

        @Test
        void process_WhenAmountExceedsLimit_ThrowsException() {
                // Arrange
                TransferRequestDTO requestDTO = new TransferRequestDTO("wallet1", "wallet2", new BigDecimal("10000.01"),
                                "Car");
                TransactionContext context = TransactionContext.builder()
                                .transactionId("TXN-12345")
                                .request(requestDTO)
                                .build();

                // Act & Assert
                assertThatThrownBy(() -> limitCheckHandler.process(context))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("Transfer amount exceeds the maximum allowed limit");
        }
}
