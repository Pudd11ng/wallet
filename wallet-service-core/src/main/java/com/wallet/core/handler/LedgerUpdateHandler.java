package com.wallet.core.handler;

import com.wallet.core.entity.Wallet;
import com.wallet.core.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Order(3) // Runs Third
@RequiredArgsConstructor
public class LedgerUpdateHandler implements TransactionHandler {

    private final WalletRepository walletRepository;

    @Override
    public void process(TransactionContext context) {
        log.info("Step 3: Updating ledger for transaction:  {}", context.getTransactionId());

        Wallet sender = context.getSenderWallet();
        Wallet receiver = context.getReceiverWallet();
        BigDecimal amount = context.getRequest().amount();

        // 1. Double-Entry Math
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        // 2. Save updates (Optimistic Locking triggers here automatically)
        walletRepository.save(sender);
        walletRepository.save(receiver);

        log.info("Ledger successfully updated for {}", context.getTransactionId());

        // Note: For full compliance, this is also where we will insert
        // the 2 rows into the `journal_entries` table.
    }
}