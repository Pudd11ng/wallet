package com.wallet.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.common.constants.WalletConstants;
import com.wallet.common.dto.*;
import com.wallet.core.facade.TransactionFacade;
import com.wallet.core.service.IdempotencyService;
import com.wallet.core.service.QrService;
import com.wallet.core.service.WalletManagementService;
import com.wallet.core.mapper.WalletMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@AutoConfigureMockMvc(addFilters = false)
class WalletControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        // Service Mocks
        @MockitoBean
        private TransactionFacade transactionFacade;

        @MockitoBean
        private WalletManagementService walletManagementService;

        @MockitoBean
        private QrService qrService;

        @MockitoBean
        private IdempotencyService idempotencyService;

        // Infrastructure Mocks
        @MockitoBean
        private WalletMapper walletMapper;

        @MockitoBean
        private KafkaTemplate<String, Object> kafkaTemplate;

        @MockitoBean
        private StringRedisTemplate stringRedisTemplate;

        @Test
        void transferFunds_WhenValidPayload_Returns200Ok() throws Exception {
                TransferRequestDTO request = new TransferRequestDTO(
                                "SENDER-123",
                                "RECEIVER-456",
                                new BigDecimal("100.00"),
                                "Test transfer");

                WalletResponseDTO response = new WalletResponseDTO(
                                "SENDER-123",
                                new BigDecimal("900.00"),
                                "USD",
                                "SUCCESS");

                when(transactionFacade.executeTransfer(eq("req-123"), eq("client-123"), any(TransferRequestDTO.class)))
                                .thenReturn(response);

                mockMvc.perform(post("/api/v1/wallets/transfer")
                                .header(WalletConstants.HEADER_REQUEST_ID, "req-123")
                                .header(WalletConstants.HEADER_CLIENT_ID, "client-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.walletId").value("SENDER-123"));
        }

        @Test
        void transferFunds_WhenMissingHeader_Returns400BadRequest() throws Exception {
                TransferRequestDTO request = new TransferRequestDTO(
                                "SENDER-123",
                                "RECEIVER-456",
                                new BigDecimal("100.00"),
                                "Test transfer");

                mockMvc.perform(post("/api/v1/wallets/transfer")
                                // Missing X-Request-ID header to trigger 400 Bad Request
                                .header(WalletConstants.HEADER_CLIENT_ID, "client-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getWalletHistory_WhenValidRequest_Returns200Ok() throws Exception {
                WalletHistoryResponseDTO response = new WalletHistoryResponseDTO(
                                "W1234",
                                new BigDecimal("100.00"),
                                "USD",
                                List.of());

                when(walletManagementService.getWalletHistory(eq("W1234"), eq("client-123")))
                                .thenReturn(response);

                mockMvc.perform(get("/api/v1/wallets/W1234/history")
                                .header(WalletConstants.HEADER_REQUEST_ID, "req-123")
                                .header(WalletConstants.HEADER_CLIENT_ID, "client-123"))
                                .andExpect(status().isOk());
        }

        @Test
        void generateQr_WhenValidRequest_Returns200Ok() throws Exception {
                String requestJson = "{}"; // Empty request payload

                when(qrService.generateQrCode(eq("client-123"), any(QrGenerateRequestDTO.class)))
                                .thenReturn("mocked-qr-string");

                mockMvc.perform(post("/api/v1/wallets/qr/generate")
                                .header(WalletConstants.HEADER_REQUEST_ID, "req-123")
                                .header("X-Client-Id", "client-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.qrData").value("mocked-qr-string"));
        }

        @Test
        void decodeQr_WhenValidRequest_Returns200Ok() throws Exception {
                String requestJson = "{}"; // Empty request payload

                when(qrService.decodeQrCode(any(QrDecodeRequestDTO.class)))
                                .thenReturn(new QrDecodeResponseDTO("W1234", BigDecimal.TEN, "VALID_AND_VERIFIED"));

                mockMvc.perform(post("/api/v1/wallets/qr/decode")
                                .header(WalletConstants.HEADER_REQUEST_ID, "req-123")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isOk());
        }
}
