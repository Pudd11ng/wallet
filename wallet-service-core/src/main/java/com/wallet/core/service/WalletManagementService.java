package com.wallet.core.service;

import com.wallet.common.dto.*;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.JournalEntry;
import com.wallet.core.entity.TransactionRequest;
import com.wallet.core.entity.Wallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.wallet.core.mapper.WalletMapper;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor

public class WalletManagementService {

    private final WalletMapper walletMapper;

    @Transactional
    public InitializeWalletResponseDTO initializeWallet(InitializeWalletRequestDTO request) {
        log.info("Initializing new wallet for User ID: {}", request.userId());

        // 1. Check if the user already has a wallet
        walletMapper.findWalletByUserId(request.userId()).ifPresent(w -> {
            throw new WalletBusinessException("User already has an active wallet: " + w.id());
        });

        // 2. Generate a secure Wallet ID (e.g., W-8F9A2B)
        String walletId = "W-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. Create the Wallet Entity
        Wallet newWallet = new Wallet(
                walletId,
                request.userId(),
                BigDecimal.ZERO.setScale(4), // Enforces 0.0000 format from SA doc
                request.currency(),
                "ACTIVE",
                0, // Initial optimistic lock version
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // 4. Save to Database using your existing MyBatis Mapper
        walletMapper.insertWallet(newWallet);
        log.info("Successfully created Wallet ID {} for User ID {}", walletId, request.userId());

        // 5. Return the Response
        return new InitializeWalletResponseDTO(
                newWallet.id(),
                newWallet.balance(),
                newWallet.currency(),
                newWallet.status()
        );
    }

    @Transactional
    public TopUpResponseDTO topUpWallet(String requestId, TopUpRequestDTO request) {
        log.info("Processing Top-Up of {} for Wallet ID: {}", request.amount(), request.walletId());

        // 1. Fetch the Wallet
        Wallet wallet = walletMapper.findWalletById(request.walletId())
                .orElseThrow(() -> new WalletBusinessException("Wallet not found: " + request.walletId()));

        if (!"ACTIVE".equals(wallet.status())) {
            throw new WalletBusinessException("Wallet is not active.");
        }

        // 2. Generate an official Transaction ID
        String transactionId = "TXN-TOPUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 3. Update Balance (Optimistic Locking)
        BigDecimal newBalance = wallet.balance().add(request.amount());
        int rowsUpdated = walletMapper.updateWalletBalance(wallet.id(), newBalance, wallet.version());

        if (rowsUpdated == 0) {
            throw new WalletBusinessException("Concurrency error: Wallet state changed during top-up. Please retry.");
        }

        // 4. Save the Idempotency Request Record
        TransactionRequest txnRequest = new TransactionRequest(
                transactionId, requestId, "TOPUP", "SUCCESS", request.amount(), LocalDateTime.now()
        );
        walletMapper.insertTransactionRequest(txnRequest);

        // 5. Save the Journal Entry (Single-Entry for external inbound funds)
        JournalEntry creditEntry = new JournalEntry(
                null, transactionId, wallet.id(), "CREDIT", request.amount(), LocalDateTime.now()
        );
        walletMapper.insertJournalEntry(creditEntry);

        log.info("Top-Up successful. New balance for {} is {}", wallet.id(), newBalance);

        // 6. Return the SA Document specified response
        return new TopUpResponseDTO(transactionId, newBalance, wallet.currency());
    }

    public WalletHistoryResponseDTO getWalletHistory(String walletId) {
        log.info("Fetching history for Wallet ID: {}", walletId);

        // 1. Fetch the Wallet to get the current balance
        Wallet wallet = walletMapper.findWalletById(walletId)
                .orElseThrow(() -> new WalletBusinessException("Wallet not found: " + walletId));

        // 2. Fetch the last 50 ledger entries
        List<JournalEntry> entries = walletMapper.findJournalEntriesByWalletId(walletId);

        // 3. Map the DB Entities to DTOs
        List<TransactionHistoryDTO> transactionHistory = entries.stream()
                .map(entry -> new TransactionHistoryDTO(
                        entry.transactionId(),
                        entry.type(),
                        entry.amount(),
                        entry.createdAt()
                )).toList();

        // 4. Return the combined response
        return new WalletHistoryResponseDTO(
                wallet.id(),
                wallet.balance(),
                wallet.currency(),
                transactionHistory
        );
    }
}
