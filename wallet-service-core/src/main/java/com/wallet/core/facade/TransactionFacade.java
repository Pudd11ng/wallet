package com.wallet.core.facade;

import com.wallet.common.dto.TransferRequestDTO;

public interface TransactionFacade {
    String executeTransfer(String requestId, String clientId, TransferRequestDTO request);
}