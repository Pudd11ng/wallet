package com.wallet.core.service;

import com.wallet.core.entity.OutboxEvent;
import com.wallet.core.mapper.WalletMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OutboxRelayService outboxRelayService;

    @Test
    void processOutboxEvents_WhenEventsExist_SendsToKafkaAndMarksProcessed() {
        // Arrange
        OutboxEvent event1 = new OutboxEvent(1L, "topic-a", "payload1", "PENDING", LocalDateTime.now());
        OutboxEvent event2 = new OutboxEvent(2L, "topic-b", "payload2", "PENDING", LocalDateTime.now());
        List<OutboxEvent> pendingEvents = List.of(event1, event2);

        when(walletMapper.findPendingOutboxEvents()).thenReturn(pendingEvents);

        // Act
        outboxRelayService.relayEventsToKafka();

        // Assert
        verify(kafkaTemplate, times(1)).send(eq("topic-a"), eq("payload1"));
        verify(walletMapper, times(1)).updateOutboxEventStatus(eq(1L), eq("SENT"));

        verify(kafkaTemplate, times(1)).send(eq("topic-b"), eq("payload2"));
        verify(walletMapper, times(1)).updateOutboxEventStatus(eq(2L), eq("SENT"));
    }

    @Test
    void processOutboxEvents_WhenKafkaFails_DoesNotMarkProcessed() {
        // Arrange
        OutboxEvent event1 = new OutboxEvent(1L, "topic-a", "payload1", "PENDING", LocalDateTime.now());
        List<OutboxEvent> pendingEvents = List.of(event1);

        when(walletMapper.findPendingOutboxEvents()).thenReturn(pendingEvents);
        when(kafkaTemplate.send(anyString(), any())).thenThrow(new RuntimeException("Kafka is down"));

        // Act
        outboxRelayService.relayEventsToKafka();

        // Assert
        verify(kafkaTemplate, times(1)).send(eq("topic-a"), eq("payload1"));
        verify(walletMapper, never()).updateOutboxEventStatus(anyLong(), anyString()); // Crucial rollback verified
    }

    @Test
    void processOutboxEvents_WhenNoEventsExists_DoesNothing() {
        // Arrange
        when(walletMapper.findPendingOutboxEvents()).thenReturn(List.of());

        // Act
        outboxRelayService.relayEventsToKafka();

        // Assert
        verify(kafkaTemplate, never()).send(anyString(), any());
        verify(walletMapper, never()).updateOutboxEventStatus(anyLong(), anyString());
    }
}
