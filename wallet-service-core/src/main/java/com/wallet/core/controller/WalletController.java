package com.wallet.core.controller;

import com.wallet.common.dto.TransferRequestDTO;
import com.wallet.common.dto.WalletResponseDTO;
import com.wallet.core.service.TransferService;
import com.wallet.core.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final TransferService transferService;
    private final IdempotencyService idempotencyService;

    @PostMapping("/transfer")
    public ResponseEntity<WalletResponseDTO> transferFunds(
            @RequestHeader(value = "X-Request-ID") String requestId,
            @Valid @RequestBody TransferRequestDTO request) {

        // -> REDIS LOCK FIRES HERE <-
        idempotencyService.checkAndLock(requestId);

        // 1. Inject the Request ID into the Logging Context (MDC)
        MDC.put("requestId", requestId);

        try {
            log.info("Received transfer HTTP request.");

            // 2. Execute Business Logic
            WalletResponseDTO response = transferService.executeTransfer(request, requestId);

            log.info("Transfer processed successfully. Sending 200 OK.");
            return ResponseEntity.ok(response);

        } finally {
            // 3. CRITICAL: Always clear the MDC context!
            // Tomcat reuses threads. If we don't clear this, the next user's
            // request might accidentally log under the previous user's ID.
            MDC.clear();
        }
    }
}