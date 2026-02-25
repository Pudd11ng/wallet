package com.wallet.core.controller;

import com.wallet.common.dto.*;
import com.wallet.core.facade.TransactionFacade;
import com.wallet.core.service.IdempotencyService;
import com.wallet.core.service.WalletManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final TransactionFacade transactionFacade;
    private final IdempotencyService idempotencyService;
    private final WalletManagementService walletManagementService;

    @PostMapping("/initialize")
    public ResponseEntity<InitializeWalletResponseDTO> initializeWallet(
            @RequestHeader(value = "X-Request-ID") String requestId,
            @RequestHeader(value = "X-Client-Id") String clientId,
            @Valid @RequestBody InitializeWalletRequestDTO request) {

        MDC.put("requestId", requestId);
        idempotencyService.checkAndLock(requestId);

        try {
            log.info("Received HTTP request to initialize wallet.");
            InitializeWalletResponseDTO response = walletManagementService.initializeWallet(request, clientId);
            return new ResponseEntity<>(response, HttpStatus.CREATED); // Returns 201 Created
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/topup")
    public ResponseEntity<TopUpResponseDTO> topUpWallet(
            @RequestHeader(value = "X-Request-ID") String requestId,
            @RequestHeader(value = "X-Client-Id") String clientId,
            @Valid @RequestBody TopUpRequestDTO request) {

        MDC.put("requestId", requestId);
        idempotencyService.checkAndLock(requestId); // Prevents duplicate Bank Top-Ups!

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
            @RequestHeader(value = "X-Request-ID") String requestId,
            @RequestHeader(value = "X-Client-Id") String clientId) {

        MDC.put("requestId", requestId);
        // Note: No idempotency lock needed here because GET requests don't change data!

        try {
            log.info("Received HTTP request for Wallet History.");
            WalletHistoryResponseDTO response = walletManagementService.getWalletHistory(walletId, clientId);
            return ResponseEntity.ok(response);
        } finally {
            MDC.clear();
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<WalletResponseDTO> transferFunds(
            @RequestHeader(value = "X-Request-ID") String requestId,
            @RequestHeader(value = "X-Client-Id") String clientId,
            @Valid @RequestBody TransferRequestDTO request) {

        MDC.put("requestId", requestId);
        idempotencyService.checkAndLock(requestId);

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
}