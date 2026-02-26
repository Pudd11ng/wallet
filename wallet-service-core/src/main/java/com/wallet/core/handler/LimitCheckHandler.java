package com.wallet.core.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@Order(2) // Runs Second
public class LimitCheckHandler implements TransactionHandler {

    // Example hardcoded limit (could be moved to application.yml or DB)
    private static final BigDecimal MAX_TRANSFER_LIMIT = new BigDecimal("10000.00");

    @Override
    public void process(TransactionContext context) {
        log.info("Step 2: Checking limits for transaction: {}", context.getTransactionId());

        BigDecimal amount = context.getRequest().amount();

        if (amount.compareTo(MAX_TRANSFER_LIMIT) > 0) {
            log.warn("Transfer amount {} exceeds maximum limit {}", amount, MAX_TRANSFER_LIMIT);
            throw new IllegalStateException("Transfer amount exceeds the maximum allowed limit");
        }

        // You could add logic here to check the DB for total transfers in the last 24 hours.
    }
}
