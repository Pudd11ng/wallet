package com.wallet.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SignatureUtilTest {

    @InjectMocks
    private SignatureUtil signatureUtil;

    private KeyPair keyPair;
    private String publicKeyBase64;

    @BeforeEach
    void setUp() throws Exception {
        // Generate a real RSA KeyPair for testing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();

        publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        // Inject the generated public key into the utility
        ReflectionTestUtils.setField(signatureUtil, "publicKeyBase64", publicKeyBase64);
    }

    private String generateTestSignature(String payload) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(payload.getBytes());
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    @Test
    void validateSignature_WhenSignatureMatches_ReturnsTrue() throws Exception {
        // Arrange
        String payload = "{\"userId\":\"U-123\",\"amount\":100.50}";
        String validSignature = generateTestSignature(payload);

        // Act
        boolean isValid = signatureUtil.verifySignature(payload, validSignature);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void validateSignature_WhenSignatureDoesNotMatch_ReturnsFalse() throws Exception {
        // Arrange
        String payload = "{\"userId\":\"U-123\",\"amount\":100.50}";
        String validSignature = generateTestSignature(payload);

        // Tamper with the payload
        String tamperedPayload = "{\"userId\":\"U-123\",\"amount\":999.99}";

        // Act
        boolean isValid = signatureUtil.verifySignature(tamperedPayload, validSignature);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void validateSignature_WhenExceptionThrown_ReturnsFalse() {
        // Arrange
        String payload = "{\"userId\":\"U-123\"}";
        // Pass an invalid Base64 string to force an Exception during decoding
        String invalidBase64Signature = "Not-A-Valid-Base64-String!!!";

        // Act
        boolean isValid = signatureUtil.verifySignature(payload, invalidBase64Signature);

        // Assert
        assertThat(isValid).isFalse();
    }
}
