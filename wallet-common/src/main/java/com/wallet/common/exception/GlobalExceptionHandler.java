package com.wallet.common.exception;

import com.wallet.common.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle our custom business exceptions
    @ExceptionHandler(WalletBusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(WalletBusinessException ex) {
        String requestId = MDC.get("requestId"); // Pulls the ID from the logging context

        log.warn("Business rule violation [{}]: {}", requestId, ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                "ERR_BUSINESS_RULE",
                ex.getMessage(),
                requestId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Handle unexpected system crashes (e.g., NullPointerException)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleSystemException(Exception ex) {
        String requestId = MDC.get("requestId");

        log.error("Critical System Error [{}]: ", requestId, ex);

        ErrorResponse response = new ErrorResponse(
                "ERR_INTERNAL_SERVER",
                "An unexpected error occurred. Please contact support with Request ID: " + requestId,
                requestId,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}