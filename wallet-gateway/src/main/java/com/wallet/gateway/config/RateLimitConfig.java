package com.wallet.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    @Bean
    public KeyResolver clientIdResolver() {
        // Extracts the "X-Client-Id" header. If missing, groups them as "ANONYMOUS"
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst("X-Client-Id"))
                .defaultIfEmpty("ANONYMOUS");
    }
}
