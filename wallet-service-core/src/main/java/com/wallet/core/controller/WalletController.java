package com.wallet.core.controller;

import com.wallet.common.constants.WalletConstants;
import com.wallet.common.dto.*;
import com.wallet.core.annotation.Idempotent;
import com.wallet.core.facade.TransactionFacade;
import com.wallet.core.service.QrService;
import com.wallet.core.service.WalletManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final TransactionFacade transactionFacade;
    private final WalletManagementService walletManagementService;
    private final QrService qrService;

    @Idempotent
    @PostMapping("/initialize")
    public ResponseEntity<InitializeWalletResponseDTO> initializeWallet(
            @RequestHeader(value = WalletConstants.HEADER_REQUEST_ID) String requestId,
            @RequestHeader(value = WalletConstants.HEADER_CLIENT_ID) String clientId,
            @Valid @RequestBody InitializeWalletRequestDTO request) {

        MDC.put("requestId", requestId);

        try {
            log.info("Received HTTP request to initialize wallet.");
            InitializeWalletResponseDTO response = walletManagementService.initializeWallet(request, clientId);
            return new ResponseEntity<>(response, HttpStatus.CREATED); // Returns 201 Created
        } finally {
            MDC.clear();
        }
    }

    @Idempotent
    @PostMapping("/topup")
    public ResponseEntity<TopUpResponseDTO> topUpWallet(
            @RequestHeader(value = WalletConstants.HEADER_REQUEST_ID) String requestId,
            @RequestHeader(value = WalletConstants.HEADER_CLIENT_ID) String clientId,
            @Valid @RequestBody TopUpRequestDTO request) {

        MDC.put("requestId", requestId);

        try {
            log.info("Received HTTP request for Wallet Top-Up.");
            TopUpResponseDTO response = walletManagementService.topUpWallet(requestId, request, clientId);
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @GetMapping("/{walletId}/history")
    public ResponseEntity<WalletHistoryResponseDTO> getWalletHistory(
            @PathVariable String walletId,
            @RequestHeader(value = WalletConstants.HEADER_REQUEST_ID) String requestId,
            @RequestHeader(value = WalletConstants.HEADER_CLIENT_ID) String clientId) {

        MDC.put("requestId", requestId);

        try {
            log.info("Received HTTP request for Wallet History.");
            WalletHistoryResponseDTO response = walletManagementService.getWalletHistory(walletId, clientId);
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @Idempotent
    @PostMapping("/transfer")
    public ResponseEntity<WalletResponseDTO> transferFunds(
            @RequestHeader(value = WalletConstants.HEADER_REQUEST_ID) String requestId,
            @RequestHeader(value = WalletConstants.HEADER_CLIENT_ID) String clientId,
            @Valid @RequestBody TransferRequestDTO request) {

        MDC.put("requestId", requestId);

        try {
            log.info("Received transfer HTTP request.");

            // Execute the strict assembly line
            WalletResponseDTO response = transactionFacade.executeTransfer(requestId, clientId, request);

            log.info("Transfer processed successfully. Sending 200 OK.");

            return ResponseEntity.ok(response);

        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/qr/generate")
    public ResponseEntity<QrGenerateResponseDTO> generateQr(
            @RequestHeader(value = WalletConstants.HEADER_REQUEST_ID) String requestId,
            @RequestHeader("X-Client-Id") String clientId,
            @RequestBody QrGenerateRequestDTO request) {

        MDC.put("requestId", requestId);

        try {
            String qrString = qrService.generateQrCode(clientId, request);
            return ResponseEntity.ok(new QrGenerateResponseDTO(qrString));

        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/qr/decode")
    public ResponseEntity<QrDecodeResponseDTO> decodeQr(
            @RequestHeader(value = WalletConstants.HEADER_REQUEST_ID) String requestId,
            @RequestBody QrDecodeRequestDTO request) {

        MDC.put("requestId", requestId);

        try {
            QrDecodeResponseDTO response = qrService.decodeQrCode(request);
            return ResponseEntity.ok(response);

        } finally {
            MDC.clear();
        }
    }
}