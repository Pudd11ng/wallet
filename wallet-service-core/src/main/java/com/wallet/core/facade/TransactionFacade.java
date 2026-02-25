package com.wallet.core.facade;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.dto.WalletResponseDTO;

public interface TransactionFacade {
    WalletResponseDTO executeTransfer(String requestId, String clientId, TransferRequestDTO request);
}