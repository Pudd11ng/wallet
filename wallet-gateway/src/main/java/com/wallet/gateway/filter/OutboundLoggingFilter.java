package com.wallet.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OutboundLoggingFilter implements GlobalFilter, Ordered {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        // --- 1. INTERCEPT THE REQUEST ---
        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody().doOnNext(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.asByteBuffer().asReadOnlyBuffer().get(bytes);
                    String bodyStr = new String(bytes, StandardCharsets.UTF_8);

                    logOutboundRequest(exchange.getRequest(), bodyStr);
                });
            }
        };

        // --- 2. INTERCEPT THE RESPONSE ---
        ServerHttpResponse decoratedResponse = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content); // Consume the buffer

                        // Rebuild the buffer for the client
                        DataBufferFactory bufferFactory = exchange.getResponse().bufferFactory();
                        DataBuffer newBuffer = bufferFactory.wrap(content);

                        String responseBody = new String(content, StandardCharsets.UTF_8);
                        long duration = System.currentTimeMillis() - startTime;

                        logInboundResponse(exchange.getResponse(), responseBody, duration);

                        return newBuffer;
                    }));
                }
                return super.writeWith(body);
            }
        };

        // Edge case: GET requests have no body, so the body interceptor won't trigger.
        // We log them immediately here.
        if ("GET".equalsIgnoreCase(method)) {
            logOutboundRequest(exchange.getRequest(), "");
        }

        return chain.filter(exchange.mutate().request(decoratedRequest).response(decoratedResponse).build());
    }

    private void logOutboundRequest(ServerHttpRequest request, String body) {
        StringBuilder sb = new StringBuilder("\n========== [OUTBOUND REQUEST TO MICROSERVICE] ==========\n");
        sb.append("Method : ").append(request.getMethod().name()).append("\n");
        sb.append("URI    : ").append(request.getURI()).append("\n");

        request.getHeaders().forEach((key, value) -> {
            sb.append("Header : ").append(key).append(" = ").append(value).append("\n");
        });

        if (!body.isEmpty()) {
            sb.append("Body   :\n").append(formatJson(body)).append("\n");
        }
        sb.append("========================================================");
        log.info(sb.toString());
    }

    private void logInboundResponse(ServerHttpResponse response, String body, long duration) {
        StringBuilder sb = new StringBuilder("\n========== [INBOUND RESPONSE FROM MICROSERVICE] ========\n");

        int status = response.getStatusCode() != null ? response.getStatusCode().value() : 500;
        sb.append("Status : ").append(status).append("\n");
        sb.append("Time   : ").append(duration).append(" ms\n");

        response.getHeaders().forEach((key, value) -> {
            sb.append("Header : ").append(key).append(" = ").append(value).append("\n");
        });

        if (!body.isEmpty()) {
            sb.append("Body   :\n").append(formatJson(body)).append("\n");
        }
        sb.append("========================================================");
        log.info(sb.toString());
    }

    // Helper to make the JSON look beautiful in the console
    private String formatJson(String rawString) {
        try {
            Object json = objectMapper.readValue(rawString, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (Exception e) {
            // If it's not JSON (like plain text), just return it as is
            return rawString;
        }
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
