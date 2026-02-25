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
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private String senderUsername;
}