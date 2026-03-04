package com.wallet.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    // A valid Base64 encoded secret for JWT (at least 256 bits for HmacSHA256)
    private static final String SECRET_BASE64 = "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET_BASE64);
    }

    private String generateValidToken(String subject) {
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET_BASE64);
        Key key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void extractUserId_WhenValidToken_ReturnsUserId() {
        // Arrange
        String expectedUserId = "U-987654321";
        String validToken = generateValidToken(expectedUserId);

        // Act
        String actualUserId = jwtUtil.extractUserId(validToken);

        // Assert
        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    @Test
    void validateToken_WhenValidToken_ReturnsTrue() {
        // Arrange
        String validToken = generateValidToken("U-12345");

        // Act & Assert
        // validateToken expects to complete normally without exception
        jwtUtil.validateToken(validToken);
    }

    @Test
    void validateToken_WhenInvalidToken_ThrowsException() {
        // Arrange
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid-payload.invalid-signature";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    void getAuthHeader_WhenHeaderExists_ReturnsBearerToken() {
        // Arrange
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer valid-jwt-token");
        when(request.getHeaders()).thenReturn(headers);

        // Act
        String token = jwtUtil.getAuthHeader(request);

        // Assert
        assertThat(token).isEqualTo("valid-jwt-token");
    }

    @Test
    void getAuthHeader_WhenHeaderMissing_ReturnsNull() {
        // Arrange
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        // Act
        String token = jwtUtil.getAuthHeader(request);

        // Assert
        assertThat(token).isNull();
    }
}
