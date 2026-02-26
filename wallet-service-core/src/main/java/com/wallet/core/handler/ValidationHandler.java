package com.wallet.core.handler;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.exception.WalletBusinessException;
import com.wallet.core.entity.Wallet;
import com.wallet.core.mapper.WalletMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ValidationHandler implements TransactionHandler {

    private final WalletMapper walletMapper; // Changed to MyBatis Mapper

    @Override
    public void process(TransactionContext context) {
        TransferRequestDTO request = context.getRequest();
        log.info("Step 1: Validating transfer request: {}", context.getRequestId());

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletBusinessException("Transfer amount must be greater than zero");
        }

        if (request.fromWalletId().equals(request.toWalletId())) {
            throw new WalletBusinessException("Cannot transfer funds to the same wallet");
        }

        Wallet sender = walletMapper.findWalletById(request.fromWalletId())
                .orElseThrow(() -> new WalletBusinessException("Sender wallet not found"));

        if (!sender.userId().equals(context.getClientId())) {
            throw new WalletBusinessException("Unauthorized: You cannot transfer funds from a wallet you do not own.");
        }

        Wallet receiver = walletMapper.findWalletById(request.toWalletId())
                .orElseThrow(() -> new WalletBusinessException("Receiver wallet not found"));

        if (sender.balance().compareTo(request.amount()) < 0) {
            throw new WalletBusinessException("Insufficient funds");
        }

        context.setSenderWallet(sender);
        context.setReceiverWallet(receiver);
    }
}