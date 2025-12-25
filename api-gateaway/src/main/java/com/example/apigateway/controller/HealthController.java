package com.example.apigateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Mono<ResponseEntity<Map<String, Object>>> root() {
        Map<String, Object> response = Map.of(
                "service", "API Gateway",
                "status", "UP",
                "timestamp", Instant.now().toString(),
                "endpoints", Map.of(
                        "orders", "/api/orders",
                        "payments", "/api/payments",
                        "restaurants", "/api/restaurants",
                        "menuItems", "/api/menu-items",
                        "users", "/api/users",
                        "addresses", "/api/addresses",
                        "preferences", "/api/preferences",
                        "favorites", "/api/favorites",
                        "couriers", "/api/couriers",
                        "deliveries", "/api/deliveries"
                )
        );

        return Mono.just(ResponseEntity.ok(response));
    }
}
