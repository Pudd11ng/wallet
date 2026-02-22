package com.wallet.core.handler;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.core.entity.Wallet;
import com.wallet.core.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Order(1) // Runs First
@RequiredArgsConstructor
public class ValidationHandler implements TransactionHandler {

    private final WalletRepository walletRepository;

    @Override
    public void process(TransactionContext context) {
        TransferRequestDTO request = context.getRequest();
        log.info("Step 1: Validating transfer request: {}", context.getRequestId());

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        if (request.fromWalletId().equals(request.toWalletId())) {
            throw new IllegalArgumentException("Cannot transfer funds to the same wallet");
        }

        Wallet sender = walletRepository.findById(request.fromWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));

        Wallet receiver = walletRepository.findById(request.toWalletId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        if (sender.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }

        // Save to context for the next handlers
        context.setSenderWallet(sender);
        context.setReceiverWallet(receiver);
    }
}