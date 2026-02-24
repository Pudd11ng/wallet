package com.wallet.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Component
public class SignatureUtil {

    // Simulated Vault: This is a dummy RSA Public Key (Base64 encoded)
    private static final String PUBLIC_KEY_BASE64 = "MIGeMA0GCSqGSIb3DQEBAQUAA4GMADCBiAKBgG16si2UY4460tL93rYELxnA6hMp\n" +
            "Jwe32s2JV2kJJ0Gy2xJj/wCpNcR+iYDwq91knVTxllpfY+UlHWjqZAP7spD4zQwk\n" +
            "UGvV3XQ9q/1RgQVeL8Hck38C6emlgQoZK88MoIADCr6mu0rT31ZeZ23mM2p7hmRp\n" +
            "1NjVwdsioNSw/tUxAgMBAAE=";

    public boolean verifySignature(String payload, String signatureBase64) {
        try {
            // 1. Strip all newlines and spaces from the Public Key
            String cleanPublicKey = PUBLIC_KEY_BASE64.replaceAll("\\s+", "");
            byte[] keyBytes = Base64.getDecoder().decode(cleanPublicKey);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);

            // 2. Initialize the RSA-256 Signature Engine
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);

            // 3. Strip all newlines and spaces from the incoming Signature
            String cleanSignature = signatureBase64.replaceAll("\\s+", "");
            byte[] signatureBytes = Base64.getDecoder().decode(cleanSignature);

            // 4. Verify the math
            signature.update(payload.getBytes());
            return signature.verify(signatureBytes);

        } catch (Exception e) {
            log.error("RSA Signature Verification Failed: {}", e.getMessage());
            return false;
        }
    }
}