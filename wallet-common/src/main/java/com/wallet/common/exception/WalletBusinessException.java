package com.wallet.common.exception;

public class WalletBusinessException extends RuntimeException {
    public WalletBusinessException(String message) {
        super(message);
    }
}