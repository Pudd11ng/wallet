package com.wallet.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.dto.TransferEventDTO;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.JournalEntry;
import com.wallet.core.entity.OutboxEvent;
import com.wallet.core.entity.TransactionRequest;
import com.wallet.core.entity.Wallet;
import com.wallet.core.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class LedgerUpdateHandler implements TransactionHandler {

    private final WalletMapper walletMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void process(TransactionContext context) {
        log.info("Step 3: Updating ledger and Outbox for transaction: {}", context.getTransactionId());

        Wallet sender = context.getSenderWallet();
        Wallet receiver = context.getReceiverWallet();
        BigDecimal amount = context.getRequest().amount();

        // 1 & 2. Optimistic Locking Updates
        int senderUpdated = walletMapper.updateWalletBalance(sender.id(), sender.balance().subtract(amount), sender.version());
        if (senderUpdated == 0) throw new WalletBusinessException("Concurrency error: Sender wallet state changed.");

        int receiverUpdated = walletMapper.updateWalletBalance(receiver.id(), receiver.balance().add(amount), receiver.version());
        if (receiverUpdated == 0) throw new WalletBusinessException("Concurrency error: Receiver wallet state changed.");

        // ---> THE FIX: Insert the TransactionRequest to satisfy the Foreign Key constraint! <---
        TransactionRequest txnRequest = new TransactionRequest(
                context.getTransactionId(),
                context.getSenderUsername(),
                "TRANSFER",
                "SUCCESS",
                amount,
                LocalDateTime.now()
        );
        walletMapper.insertTransactionRequest(txnRequest);

        // 3. Insert Immutable Journal Entries
        walletMapper.insertJournalEntry(new JournalEntry(null, context.getTransactionId(), sender.id(), "DEBIT", amount, LocalDateTime.now()));
        walletMapper.insertJournalEntry(new JournalEntry(null, context.getTransactionId(), receiver.id(), "CREDIT", amount, LocalDateTime.now()));

        // 4. THE OUTBOX PATTERN
        try {
            TransferEventDTO eventDto = new TransferEventDTO(context.getTransactionId(), sender.id(), receiver.id(), amount, "SUCCESS");
            String jsonPayload = objectMapper.writeValueAsString(eventDto);

            OutboxEvent outboxEvent = new OutboxEvent(null, "transfer-events", jsonPayload, "PENDING", LocalDateTime.now());
            walletMapper.insertOutboxEvent(outboxEvent);

            log.info("Outbox event created for {}", context.getTransactionId());
        } catch (Exception e) {
            log.error("Failed to create outbox event", e);
            throw new WalletBusinessException("Failed to serialize outbox event");
        }
    }
}