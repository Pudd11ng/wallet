package com.wallet.notification.service;

import com.wallet.common.dto.TransferEventDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    void handleTransferEvent_WhenValidEventReceived_LogsSuccessfully() {
        // Arrange
        TransferEventDTO event = new TransferEventDTO(
                "TXN-12345",
                "W-SENDER",
                "W-RECEIVER",
                new BigDecimal("150.00"),
                "SUCCESS");

        // Act & Assert
        assertThatCode(() -> notificationListener.handleTransferEvent(event))
                .doesNotThrowAnyException();
    }
}