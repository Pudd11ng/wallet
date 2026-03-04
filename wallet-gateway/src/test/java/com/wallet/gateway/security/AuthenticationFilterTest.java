package com.wallet.gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private SignatureUtil signatureUtil;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthenticationFilter authenticationFilter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        headers = new HttpHeaders();
    }

    private void setupErrorResponse() {
        when(exchange.getResponse()).thenReturn(response);
        when(response.setComplete()).thenReturn(Mono.empty());
    }

    @Test
    void filter_WhenPathIsPublic_ProceedsWithoutAuth() {
       
        when(exchange.getRequest()).thenReturn(request);
        lenient().when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/v1/auth/login"));
        when(request.getHeaders()).thenReturn(headers); // Returns empty headers
        lenient().when(exchange.getResponse()).thenReturn(response);
        lenient().when(response.setComplete()).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
    }

    @Test
    void filter_WhenMissingAuthHeader_ReturnsUnauthorized() {
        // Arrange
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers); // empty headers
        setupErrorResponse();

        // Act
        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void filter_WhenInvalidToken_ReturnsUnauthorized() {
        // Arrange
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer invalid");
        headers.add("Request-Time", String.valueOf(System.currentTimeMillis()));
        headers.add("Nonce", "12345");
        headers.add("Signature", "algorithm=RSA256,signature=validBase64Sig!@#");

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);

        setupErrorResponse();

        // Prompt requests mocking validateToken
        doThrow(new RuntimeException("Fake Invalid JWT Exception")).when(jwtUtil).validateToken("invalid");

        // Act
        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void filter_WhenValidToken_MutatesRequestAndProceeds() {
        // Arrange
        long currentTime = System.currentTimeMillis();
        String validToken = "valid.jwt.token";
        String nonce = "uniq-nonce-999";

        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + validToken);
        headers.add("Request-Time", String.valueOf(currentTime));
        headers.add("Nonce", nonce);
        headers.add("Signature", "algorithm=RSA256,signature=validBase64Sig!@#");

        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/v1/wallets/transfer"));

        // Mock JWT
        doNothing().when(jwtUtil).validateToken(validToken);
        when(jwtUtil.extractUserId(validToken)).thenReturn("U-123");

        // Mock Redis Nonce check
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("NONCE:" + nonce), eq("USED"), any(Duration.class)))
                .thenReturn(Mono.just(true));

        // Mock Signature check
        String payloadToVerify = "/api/v1/wallets/transfer|" + currentTime + "|" + nonce + "|U-123";
        when(signatureUtil.verifySignature(payloadToVerify, "validBase64Sig!@#")).thenReturn(true);

        // Mock Mutation Setup
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        when(request.mutate()).thenReturn(requestBuilder);
        when(requestBuilder.header("X-Client-Id", "U-123")).thenReturn(requestBuilder);

        ServerHttpRequest mutatedRequest = mock(ServerHttpRequest.class);
        when(requestBuilder.build()).thenReturn(mutatedRequest);

        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(mutatedRequest)).thenReturn(exchangeBuilder);

        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);

        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = authenticationFilter.apply(new AuthenticationFilter.Config()).filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(requestBuilder).header("X-Client-Id", "U-123");
        verify(chain).filter(mutatedExchange);
    }
}
