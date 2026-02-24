package com.wallet.gateway.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final SignatureUtil signatureUtil;
    private final ReactiveStringRedisTemplate redisTemplate; // Reactive Redis for the Gateway!

    public AuthenticationFilter(JwtUtil jwtUtil, SignatureUtil signatureUtil, ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.signatureUtil = signatureUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();

            // --- 1. EXTRACT HEADERS ---
            String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
            String requestTime = headers.getFirst("Request-Time");
            String nonce = headers.getFirst("Nonce");
            String rawSignatureHeader = headers.getFirst("Signature"); // e.g., algorithm=RSA256,keyVersion=1,signature=abc...

            if (authHeader == null || requestTime == null || nonce == null || rawSignatureHeader == null) {
                return onError(exchange, "Missing mandatory security headers", HttpStatus.FORBIDDEN);
            }

            // --- 2. PARSE THE ADVANCED SIGNATURE HEADER ---
            String extractedSignature = null;
            String[] sigParts = rawSignatureHeader.split(",");

            for (String part : sigParts) {
                String trimmedPart = part.trim();
                if (trimmedPart.startsWith("signature=")) {
                    extractedSignature = trimmedPart.substring(10); // Grabs everything after "signature="
                } else if (trimmedPart.startsWith("algorithm=") && !trimmedPart.contains("RSA256")) {
                    log.warn("Rejected unsupported signature algorithm: {}", trimmedPart);
                    return onError(exchange, "Unsupported algorithm. Must be RSA256", HttpStatus.FORBIDDEN);
                }
            }

            if (extractedSignature == null) {
                return onError(exchange, "Malformed Signature header. Missing 'signature=' key.", HttpStatus.FORBIDDEN);
            }

            // --- 3. TIME WINDOW CHECK ---
            long currentTime = System.currentTimeMillis();
            long clientTime;
            try {
                clientTime = Long.parseLong(requestTime);
            } catch (NumberFormatException e) {
                return onError(exchange, "Invalid Request-Time format", HttpStatus.BAD_REQUEST);
            }

            if (Math.abs(currentTime - clientTime) > 300000) { // 5 minutes
                log.warn("Request rejected: Timestamp outside the 5-minute window");
                return onError(exchange, "Request expired", HttpStatus.FORBIDDEN);
            }

            // --- 4. JWT VALIDATION ---
            String token = authHeader.replace("Bearer ", "");
            String userId;
            try {
                jwtUtil.validateToken(token);
                userId = jwtUtil.extractUserId(token);
            } catch (Exception e) {
                return onError(exchange, "Invalid JWT", HttpStatus.UNAUTHORIZED);
            }

            // --- 5. REDIS NONCE LOCK ---
            String redisNonceKey = "NONCE:" + nonce;
            // Need to declare this final so the lambda can use it
            final String finalExtractedSignature = extractedSignature;

            return redisTemplate.opsForValue().setIfAbsent(redisNonceKey, "USED", Duration.ofMinutes(5))
                    .flatMap(isUnique -> {
                        if (Boolean.FALSE.equals(isUnique)) {
                            log.warn("Replay attack detected! Nonce {} was already used.", nonce);
                            return onError(exchange, "Duplicate Request Detected", HttpStatus.FORBIDDEN);
                        }

                        // --- 6. RSA-256 SIGNATURE VERIFICATION ---
                        String uri = exchange.getRequest().getURI().getPath();
                        String payloadToVerify = uri + "|" + requestTime + "|" + nonce + "|" + userId;

                        // Note: Uncomment this block to strictly enforce the math!

                        boolean isSignatureValid = signatureUtil.verifySignature(payloadToVerify, finalExtractedSignature);
                        if (!isSignatureValid) {
                            log.warn("Invalid RSA Signature detected for payload: {}", payloadToVerify);
                            return onError(exchange, "Payload integrity check failed", HttpStatus.FORBIDDEN);
                        }

                        exchange.getRequest().mutate()
                                .header("X-Client-Id", userId)
                                .build();

                        return chain.filter(exchange);
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {}
}
