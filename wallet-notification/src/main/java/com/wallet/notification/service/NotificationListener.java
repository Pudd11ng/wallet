package com.wallet.notification.service;

import com.wallet.common.dto.TransferEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationListener {

    @KafkaListener(topics = "transfer-events", groupId = "notification-group")
    public void handleTransferEvent(TransferEventDTO event) {
        log.info("🔔===============================================🔔");
        log.info("   NOTIFICATION SERVICE TRIGGERED!");
        log.info("   Transaction ID: {}", event.transactionId());
        log.info("   Status: {}", event.status());
        log.info("   Sending Email to User of Wallet: {}", event.toWalletId());
        log.info("   Message: 'You just received {} MYR from {}!'", event.amount(), event.fromWalletId());
        log.info("🔔===============================================🔔");
    }
}