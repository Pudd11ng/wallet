package com.wallet.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Order(2) // Runs Second
public class LimitCheckHandler implements TransactionHandler {

    @Value("${app.transfer.max-limit}")
    private BigDecimal maxTransferLimit;

    @Override
    public void process(TransactionContext context) {
        log.info("Step 2: Checking limits for transaction: {}", context.getTransactionId());

        BigDecimal amount = context.getRequest().amount();

        if (amount.compareTo(maxTransferLimit) > 0) {
            log.warn("Transfer amount {} exceeds maximum limit {}", amount, maxTransferLimit);
            throw new IllegalStateException("Transfer amount exceeds the maximum allowed limit");
        }

        // You could add logic here to check the DB for total transfers in the last 24 hours.
    }
}
