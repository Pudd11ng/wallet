// wallet-service-core/src/main/java/com/wallet/core/strategy/InternalHmacQrStrategy.java
package com.wallet.core.strategy;

import com.wallet.common.dto.QrDecodeResponseDTO;
import com.wallet.common.enums.QrFormat;
import com.wallet.common.exception.WalletBusinessException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;

@Component
public class InternalHmacQrStrategy implements QrProcessingStrategy {

    // In production, this lives in AWS KMS or HashiCorp Vault.
    private static final String QR_SECRET = "SuperSecretBankKey123!@#";

    @Override
    public QrFormat getSupportedFormat() {
        return QrFormat.INTERNAL_HMAC;
    }

    @Override
    public String generate(String walletId, BigDecimal amount) {
        // 1. Create the base payload. Expiration = 5 minutes from now
        long expiration = Instant.now().getEpochSecond() + 300;
        String payload = walletId + "|" + amount.toString() + "|" + expiration;

        // 2. Cryptographically sign the payload
        String signature = generateHmac(payload);

        // 3. Return the final string format: payload|signature
        return payload + "|" + signature;
    }

    @Override
    public QrDecodeResponseDTO decodeAndVerify(String qrString) {
        // 1. Split the string back into its parts
        String[] parts = qrString.split("\\|");
        if (parts.length != 4) {
            throw new WalletBusinessException("Invalid QR Code format");
        }

        String walletId = parts[0];
        BigDecimal amount = new BigDecimal(parts[1]);
        long expiration = Long.parseLong(parts[2]);
        String providedSignature = parts[3];

        // 2. Reconstruct the payload and hash it ourselves
        String payloadToVerify = walletId + "|" + amount.toString() + "|" + expiration;
        String expectedSignature = generateHmac(payloadToVerify);

        // 3. Compare the hashes. If they don't match, a hacker tampered with the amount!
        if (!expectedSignature.equals(providedSignature)) {
            throw new WalletBusinessException("SECURITY ALERT: QR Code signature is invalid or tampered with!");
        }

        // 4. Check if the QR code is too old
        if (Instant.now().getEpochSecond() > expiration) {
            throw new WalletBusinessException("This QR Code has expired.");
        }

        return new QrDecodeResponseDTO(walletId, amount, "VALID_AND_VERIFIED");
    }

    // Standard Java Cryptography helper for HMAC-SHA256
    private String generateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            //TODO
            SecretKeySpec secretKey = new SecretKeySpec(QR_SECRET.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            //TODO
            throw new RuntimeException("Failed to generate QR signature", e);
        }
    }
}