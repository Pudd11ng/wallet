package com.wallet.core.service;

import com.wallet.core.entity.OutboxEvent;
import com.wallet.core.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayService {

    private final WalletMapper walletMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // This tells Spring to run this method every 5000 milliseconds (5 seconds)
    @Scheduled(fixedDelay = 5000)
    public void relayEventsToKafka() {

        // 1. Fetch up to 50 pending events
        List<OutboxEvent> pendingEvents = walletMapper.findPendingOutboxEvents();

        if (pendingEvents.isEmpty()) {
            return; // Go back to sleep if there's nothing to do
        }

        log.info("Found {} pending outbox events. Relaying to Kafka...", pendingEvents.size());

        // 2. Process each event
        for (OutboxEvent event : pendingEvents) {
            try {
                // Safely send the JSON payload to Kafka
                kafkaTemplate.send(event.topic(), event.payload());

                // If Kafka successfully receives it, mark it as SENT in the database
                walletMapper.updateOutboxEventStatus(event.id(), "SENT");

                log.debug("Successfully relayed outbox event ID: {}", event.id());

            } catch (Exception e) {
                // If Kafka is down, the system catches the error.
                // The status remains 'PENDING', and it will automatically retry in 5 seconds!
                log.error("Failed to relay outbox event ID: {}. Will retry next cycle.", event.id(), e);
            }
        }
    }
}