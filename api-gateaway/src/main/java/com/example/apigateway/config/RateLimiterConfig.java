package com.example.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // Try to get user ID from JWT token
            return exchange.getPrincipal()
                    .map(principal -> principal.getName())
                    .defaultIfEmpty(
                            // Fallback to IP address for unauthenticated requests
                            Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                                    .getAddress()
                                    .getHostAddress()
                    );
        };
    }

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
                        .getAddress()
                        .getHostAddress()
        );
    }
}
