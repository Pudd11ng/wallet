package com.wallet.core.handler;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.core.entity.Wallet;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionContext {
    private String requestId;
    private String transactionId;
    private String clientId;
    private TransferRequestDTO request;

    // These will be filled in by the ValidationHandler as it moves down the chain
    private Wallet senderWallet;
    private Wallet receiverWallet;
}