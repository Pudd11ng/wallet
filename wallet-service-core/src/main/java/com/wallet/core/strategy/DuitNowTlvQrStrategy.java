package com.wallet.core.strategy;

import com.wallet.common.dto.QrDecodeResponseDTO;
import com.wallet.common.enums.QrFormat;
import com.wallet.common.exception.WalletBusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
public class DuitNowTlvQrStrategy implements QrProcessingStrategy {

    private static final String EMVCO_HEADER = "000201";

    @Override
    public QrFormat getSupportedFormat() {
        return QrFormat.DUITNOW_TLV;
    }

    @Override
    public String generate(String walletId, BigDecimal amount) {
        String target = walletId != null ? walletId : "UNKNOWN";
        String amtString = amount != null ? amount.toPlainString() : "0.00";

        StringBuilder payload = new StringBuilder();

        // Tag 00: Payload Format Indicator
        payload.append(buildTlv("00", "01"));

        // Tag 01: Point of Initiation Method (11 = Static QR)
        payload.append(buildTlv("01", "11"));

        // Tag 26: Merchant Account Information (DuitNow specific)
        String merchantGuid = buildTlv("00", "MY.COM.DUITNOW");
        String merchantId = buildTlv("01", target);
        payload.append(buildTlv("26", merchantGuid + merchantId));

        // Tag 54: Transaction Amount
        payload.append(buildTlv("54", amtString));

        // Tag 63: CRC (Checksum)
        payload.append("6304");
        String crcChecksum = calculateCrc16(payload.toString());
        payload.append(crcChecksum);

        return payload.toString();
    }

    @Override
    public QrDecodeResponseDTO decodeAndVerify(String qrData) {
        if (qrData == null || !qrData.startsWith(EMVCO_HEADER)) {
            throw new WalletBusinessException("Invalid DuitNow EMVCo TLV format");
        }

        // 1. Verify the CRC Checksum mathematically before trusting the data
        // The CRC is calculated on the entire string EXCEPT the last 4 checksum
        // characters
        String payloadWithoutCrc = qrData.substring(0, qrData.length() - 4);
        String providedCrc = qrData.substring(qrData.length() - 4);
        String expectedCrc = calculateCrc16(payloadWithoutCrc);

        if (!expectedCrc.equals(providedCrc)) {
            throw new WalletBusinessException("SECURITY ALERT: Invalid QR signature (CRC failed)");
        }

        // 2. Parse the root EMVCo String into a Map
        Map<String, String> rootTlv = parseTlv(qrData);

        // 3. Extract the Amount (Tag 54)
        String amountStr = rootTlv.get("54");
        BigDecimal amount = amountStr != null ? new BigDecimal(amountStr) : BigDecimal.ZERO;

        // 4. Extract Merchant ID (Nested inside Tag 26 -> Sub-Tag 01)
        String merchantId = "UNKNOWN";
        String tag26Value = rootTlv.get("26");
        if (tag26Value != null) {
            Map<String, String> merchantTlv = parseTlv(tag26Value);
            merchantId = merchantTlv.getOrDefault("01", "UNKNOWN");
        }

        return new QrDecodeResponseDTO(merchantId, amount, "DUITNOW_VERIFIED");
    }

    /**
     * Dynamic TLV Parser: Walks through the string using a cursor (i)
     * extracting Tag (2 chars), Length (2 chars), and Value (Length chars).
     */
    private Map<String, String> parseTlv(String tlvString) {
        Map<String, String> tlvMap = new HashMap<>();
        int i = 0;
        try {
            while (i < tlvString.length()) {
                String tag = tlvString.substring(i, i + 2);
                int length = Integer.parseInt(tlvString.substring(i + 2, i + 4));
                i += 4;
                String value = tlvString.substring(i, i + length);
                tlvMap.put(tag, value);
                i += length; // Move cursor past the value to the next Tag
            }
        } catch (Exception e) {
            throw new WalletBusinessException("Failed to parse TLV data stream");
        }
        return tlvMap;
    }

    private String buildTlv(String tag, String value) {
        String length = String.format("%02d", value.length());
        return tag + length + value;
    }

    private String calculateCrc16(String payload) {
        int crc = 0xFFFF;
        int polynomial = 0x1021;

        for (byte b : payload.getBytes()) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }
        crc &= 0xFFFF;
        return String.format("%04X", crc);
    }
}