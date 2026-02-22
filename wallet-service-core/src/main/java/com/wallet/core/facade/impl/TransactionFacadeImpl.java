package com.wallet.core.facade.impl;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.core.facade.TransactionFacade;
import com.wallet.core.handler.TransactionContext;
import com.wallet.core.handler.TransactionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionFacadeImpl implements TransactionFacade {

    private final List<TransactionHandler> handlerChain;

    @Override
    @Transactional
    public String executeTransfer(String requestId, TransferRequestDTO request) {

        // 1. Generate the official Transaction ID (TXN-UUID)
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 2. Initialize the shared Context bucket
        TransactionContext context = TransactionContext.builder()
                .requestId(requestId)
                .transactionId(transactionId)
                .request(request)
                .build();

        log.info("Starting Handler Chain for Transaction: {}", transactionId);

        // 3. Push the context through the assembly line
        for (TransactionHandler handler : handlerChain) {
            log.debug("Executing: {}", handler.getClass().getSimpleName());
            handler.process(context);
        }

        log.info("Transaction {} completed all handlers successfully.", transactionId);

        return transactionId;
    }
}