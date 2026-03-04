package com.wallet.gateway.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboundLoggingFilterTest {

    @InjectMocks
    private OutboundLoggingFilter outboundLoggingFilter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Test
    void filter_WhenCalled_LogsAndProceeds() {
        // Arrange
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/v1/test"));
        when(request.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());

        // Mock Mutations
        ServerHttpRequest.Builder requestBuilder = mock(ServerHttpRequest.Builder.class);
        // Since decorators are used in class instead of build mutations directly on the builder,
        // mock the mutate behavior on exchange returning its builder.
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        
        when(exchangeBuilder.request(any(ServerHttpRequest.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.response(any(ServerHttpResponse.class))).thenReturn(exchangeBuilder);
        
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);

        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = outboundLoggingFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        verify(chain).filter(mutatedExchange);
    }
}
