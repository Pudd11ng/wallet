package com.wallet.core.controller;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.dto.WalletResponseDTO;
import com.wallet.core.facade.TransactionFacade;
import com.wallet.core.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final TransactionFacade transactionFacade; // Switched to Facade!
    private final IdempotencyService idempotencyService;

    @PostMapping("/transfer")
    public ResponseEntity<WalletResponseDTO> transferFunds(
            @RequestHeader(value = "X-Request-ID") String requestId,
            @Valid @RequestBody TransferRequestDTO request) {

        MDC.put("requestId", requestId);
        idempotencyService.checkAndLock(requestId);

        try {
            log.info("Received transfer HTTP request.");

            // Execute the strict assembly line
            String transactionId = transactionFacade.executeTransfer(requestId, request);

            log.info("Transfer processed successfully. Sending 200 OK.");

            // We return a generic response for now (we will improve this when we add the History API)
            return ResponseEntity.ok(new WalletResponseDTO(request.fromWalletId(), BigDecimal.ZERO, "MYR", "COMPLETED"));

        } finally {
            MDC.clear();
        }
    }
}