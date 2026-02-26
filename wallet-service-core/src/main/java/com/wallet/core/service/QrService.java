package com.wallet.core.service;

import com.wallet.common.dto.QrDecodeRequestDTO;
import com.wallet.common.dto.QrDecodeResponseDTO;
import com.wallet.common.dto.QrGenerateRequestDTO;
import com.wallet.core.strategy.QrProcessingStrategy;
import com.wallet.core.factory.QrStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrService {

    private final QrStrategyFactory qrStrategyFactory;
    private final WalletManagementService walletManagementService;

    public String generateQrCode(String clientId, QrGenerateRequestDTO request) {
        log.info("Orchestrating QR Code generation for client: {}", clientId);
        String walletId = walletManagementService.getWalletIdByUserId(clientId);
        QrProcessingStrategy strategy = qrStrategyFactory.getStrategy(request.format());
        return strategy.generate(walletId, request.amount());
    }

    public QrDecodeResponseDTO decodeQrCode(QrDecodeRequestDTO request) {
        log.info("Orchestrating QR Code decoding");
        QrProcessingStrategy strategy = qrStrategyFactory.getStrategy(request.format());
        return strategy.decodeAndVerify(request.qrString());
    }
}
