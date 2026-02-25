package com.wallet.core.facade.impl;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.dto.WalletResponseDTO;
import com.wallet.core.facade.TransactionFacade;
import com.wallet.core.facade.AuthFacade;
import com.wallet.core.handler.TransactionContext;
import com.wallet.core.handler.TransactionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionFacadeImpl implements TransactionFacade {

    private final List<TransactionHandler> handlerChain;
    private final AuthFacade authFacade;

    @Override
    @Transactional
    public WalletResponseDTO executeTransfer(String requestId, String clientId, TransferRequestDTO request) {

        // 1. Generate the official Transaction ID (TXN-UUID)
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 1.1. Fetch the username
        String username = authFacade.fetchUsername(clientId);

        // 2. Initialize the shared Context bucket
        TransactionContext context = TransactionContext.builder()
                .requestId(requestId)
                .transactionId(transactionId)
                .clientId(clientId)
                .request(request)
                .senderUsername(username)
                .build();

        log.info("Starting Handler Chain for Transaction: {}", transactionId);

        // 3. Push the context through the assembly line
        for (TransactionHandler handler : handlerChain) {
            log.debug("Executing: {}", handler.getClass().getSimpleName());
            handler.process(context);
        }

        // 4. THE FIX: Calculate the new balance from the database state in the context
        BigDecimal newBalance = context.getSenderWallet().balance().subtract(request.amount());

        // 5. Return the accurate response
        return new WalletResponseDTO(
                context.getSenderWallet().id(),
                newBalance,
                context.getSenderWallet().currency(),
                "COMPLETED"
        );
    }
}