package com.wallet.core.handler;

import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.JournalEntry;
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

    private final WalletMapper walletMapper; // Changed to MyBatis Mapper

    @Override
    public void process(TransactionContext context) {
        log.info("Step 3: Updating ledger for transaction: {}", context.getTransactionId());

        Wallet sender = context.getSenderWallet();
        Wallet receiver = context.getReceiverWallet();
        BigDecimal amount = context.getRequest().amount();

        // 1. Execute Optimistic Locking Update for Sender
        int senderUpdated = walletMapper.updateWalletBalance(
                sender.id(),
                sender.balance().subtract(amount),
                sender.version()
        );
        if (senderUpdated == 0) {
            throw new WalletBusinessException("Concurrency error: Sender wallet state changed. Please retry.");
        }

        // 2. Execute Optimistic Locking Update for Receiver
        int receiverUpdated = walletMapper.updateWalletBalance(
                receiver.id(),
                receiver.balance().add(amount),
                receiver.version()
        );
        if (receiverUpdated == 0) {
            throw new WalletBusinessException("Concurrency error: Receiver wallet state changed. Please retry.");
        }

        // 3. Insert Immutable Journal Entries
        JournalEntry debitEntry = new JournalEntry(null, context.getTransactionId(), sender.id(), "DEBIT", amount, LocalDateTime.now());
        JournalEntry creditEntry = new JournalEntry(null, context.getTransactionId(), receiver.id(), "CREDIT", amount, LocalDateTime.now());

        walletMapper.insertJournalEntry(debitEntry);
        walletMapper.insertJournalEntry(creditEntry);

        log.info("Ledger successfully updated for {}", context.getTransactionId());
    }
}