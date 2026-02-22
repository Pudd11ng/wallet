package com.wallet.core.service;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.dto.WalletResponseDTO;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.JournalEntry;
import com.wallet.core.entity.TransactionRequest;
import com.wallet.core.entity.Wallet;
import com.wallet.core.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import com.wallet.common.dto.TransferEventDTO;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletMapper walletMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // The @Transactional annotation is the most important line here!
    // It guarantees ACID compliance (Atomicity). Either ALL the SQL statements succeed, or NONE of them do.
    @Transactional(rollbackFor = Exception.class)
    public WalletResponseDTO executeTransfer(TransferRequestDTO request, String requestId) {
        log.info("Starting transfer of {} from {} to {}. RequestID: {}",
                request.amount(), request.fromWalletId(), request.toWalletId(), requestId);

        // 1. Idempotency Check: Did we already process this exact request?
        walletMapper.findTransactionByRequestId(requestId).ifPresent(txn -> {
            throw new WalletBusinessException("Transaction already processed with Request ID: " + requestId);
        });

        // 2. Fetch both wallets from the database
        Wallet sender = walletMapper.findWalletById(request.fromWalletId())
                .orElseThrow(() -> new WalletBusinessException("Sender wallet not found"));

        Wallet receiver = walletMapper.findWalletById(request.toWalletId())
                .orElseThrow(() -> new WalletBusinessException("Receiver wallet not found"));

        // 3. Business Validation: Does the sender have enough money?
        if (sender.balance().compareTo(request.amount()) < 0) {
            throw new WalletBusinessException("Insufficient funds in sender wallet");
        }

        if (!"ACTIVE".equals(sender.status()) || !"ACTIVE".equals(receiver.status())) {
            throw new WalletBusinessException("One or both wallets are not active");
        }

        // 4. Record the Transaction Intent
        String transactionId = UUID.randomUUID().toString();
        TransactionRequest txnRequest = new TransactionRequest(
                transactionId, requestId, "TRANSFER", "SUCCESS", request.amount(), LocalDateTime.now()
        );
        walletMapper.insertTransactionRequest(txnRequest);

        // 5. Execute Optimistic Locking Update for Sender
        int senderUpdated = walletMapper.updateWalletBalance(
                sender.id(),
                sender.balance().subtract(request.amount()),
                sender.version()
        );
        if (senderUpdated == 0) {
            // Someone else updated the sender's balance at the exact same millisecond!
            throw new WalletBusinessException("Concurrency error: Sender wallet state changed. Please retry.");
        }

        // 6. Execute Optimistic Locking Update for Receiver
        int receiverUpdated = walletMapper.updateWalletBalance(
                receiver.id(),
                receiver.balance().add(request.amount()),
                receiver.version()
        );
        if (receiverUpdated == 0) {
            throw new WalletBusinessException("Concurrency error: Receiver wallet state changed. Please retry.");
        }

        // 7. Double-Entry Bookkeeping: Write the immutable journal ledger
        JournalEntry debitEntry = new JournalEntry(null, transactionId, sender.id(), "DEBIT", request.amount(), LocalDateTime.now());
        JournalEntry creditEntry = new JournalEntry(null, transactionId, receiver.id(), "CREDIT", request.amount(), LocalDateTime.now());

        walletMapper.insertJournalEntry(debitEntry);
        walletMapper.insertJournalEntry(creditEntry);

        log.info("Transfer {} completed successfully", transactionId);

        // --- KAFKA CODE ---
        TransferEventDTO event = new TransferEventDTO(
                transactionId, sender.id(), receiver.id(), request.amount(), "SUCCESS"
        );

        // Publish to the "transfer-events" topic
        kafkaTemplate.send("transfer-events", transactionId, event);
        log.info("Published TransferEvent to Kafka for transaction: {}", transactionId);
        // ----------------------

        // 8. Return the new state safely hidden in a DTO
        return new WalletResponseDTO(
                sender.id(),
                sender.balance().subtract(request.amount()),
                sender.currency(),
                sender.status()
        );
    }
}