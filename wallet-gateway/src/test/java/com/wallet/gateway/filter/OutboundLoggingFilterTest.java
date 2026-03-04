package com.wallet.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundLoggingFilterTest {

    @Mock
    private ObjectMapper objectMapper;

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
    void filter_WhenPostRequest_DecoratesAndProceeds() {
        // Arrange
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getMethod()).thenReturn(HttpMethod.POST);
        
        // Mock the complex exchange mutation builder chain
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequestDecorator.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.response(any(ServerHttpResponseDecorator.class))).thenReturn(exchangeBuilder);
        
        ServerWebExchange mutatedExchange = mock(ServerWebExchange.class);
        when(exchangeBuilder.build()).thenReturn(mutatedExchange);
        
        when(chain.filter(mutatedExchange)).thenReturn(Mono.empty());

        // Act
        Mono<Void> result = outboundLoggingFilter.filter(exchange, chain);

        // Assert
        StepVerifier.create(result).verifyComplete();
        
        // Verify the chain was continued with the newly mutated (decorated) exchange
        verify(chain).filter(mutatedExchange);
    }

    @Test
    void filter_WhenGetRequest_LogsImmediatelyAndProceeds() {
        // Arrange
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        
        // Because it's a GET request, the code will immediately attempt to log the URI and Headers
        when(request.getURI()).thenReturn(URI.create("http://localhost:8080/api/test"));
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        // Mock the complex exchange mutation builder chain
        ServerWebExchange.Builder exchangeBuilder = mock(ServerWebExchange.Builder.class);
        when(exchange.mutate()).thenReturn(exchangeBuilder);
        when(exchangeBuilder.request(any(ServerHttpRequestDecorator.class))).thenReturn(exchangeBuilder);
        when(exchangeBuilder.response(any(ServerHttpResponseDecorator.class))).thenReturn(exchangeBuilder);
        
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
