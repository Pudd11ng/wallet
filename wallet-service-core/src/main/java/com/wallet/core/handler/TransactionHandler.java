package com.wallet.core.handler;

public interface TransactionHandler {

    // Every handler will implement this method
    void process(TransactionContext context);

}