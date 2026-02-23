package com.wallet.core.mapper;

import com.wallet.core.entity.Wallet;
import com.wallet.core.entity.TransactionRequest;
import com.wallet.core.entity.JournalEntry;
import com.wallet.core.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Optional;

@Mapper
public interface WalletMapper {

    //1. Wallet Queries
    void insertWallet(Wallet wallet);
    Optional<Wallet> findWalletByUserId(@Param("userId") String userId);
    Optional<Wallet> findWalletById(@Param("id") String id);

    // The Critical update method with optimistic locking
// The Critical Optimistic Locking Update
    int updateWalletBalance(
            @Param("walletId") String walletId,
            @Param("newBalance") BigDecimal newBalance,
            @Param("oldVersion") Integer oldVersion
    );

    // 2. Idempotency Queries
    void insertTransactionRequest(TransactionRequest request);
    Optional<TransactionRequest> findTransactionByRequestId(@Param("requestId") String request);

    // 3. Ledger Queries
    void insertJournalEntry(JournalEntry entry);

    // 4. Outbox Queries
    void insertOutboxEvent(OutboxEvent event);
    java.util.List<OutboxEvent> findPendingOutboxEvents();
    void updateOutboxEventStatus(@Param("id") Long id, @Param("status") String status);
}
