package com.example.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(request)
                .build();

        mutatedExchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String finalRequestId = requestId;

        log.info("[{}] Incoming request: {} {}", finalRequestId, method, path);

        return chain.filter(mutatedExchange)
                .then(Mono.fromRunnable(() -> {
                    Long startTime = mutatedExchange.getAttribute(START_TIME_ATTR);
                    if (startTime != null) {
                        long duration = System.currentTimeMillis() - startTime;
                        int statusCode = mutatedExchange.getResponse().getStatusCode() != null
                                ? mutatedExchange.getResponse().getStatusCode().value()
                                : 0;
                        log.info("[{}] Completed request: {} {} - Status: {} - Duration: {}ms",
                                finalRequestId, method, path, statusCode, duration);
                    }
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
