package com.example.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> ordersFallback() {
        log.warn("Order service is unavailable, returning fallback response");
        return createFallbackResponse("Order service is temporarily unavailable");
    }

    @GetMapping("/payments")
    public Mono<ResponseEntity<Map<String, Object>>> paymentsFallback() {
        log.warn("Payment service is unavailable, returning fallback response");
        return createFallbackResponse("Payment service is temporarily unavailable");
    }

    @GetMapping("/restaurants")
    public Mono<ResponseEntity<Map<String, Object>>> restaurantsFallback() {
        log.warn("Restaurant service is unavailable, returning fallback response");
        return createFallbackResponse("Restaurant service is temporarily unavailable");
    }

    @GetMapping("/menu")
    public Mono<ResponseEntity<Map<String, Object>>> menuFallback() {
        log.warn("Menu service is unavailable, returning fallback response");
        return createFallbackResponse("Menu service is temporarily unavailable");
    }

    @GetMapping("/restaurant-orders")
    public Mono<ResponseEntity<Map<String, Object>>> restaurantOrdersFallback() {
        log.warn("Restaurant orders service is unavailable, returning fallback response");
        return createFallbackResponse("Restaurant orders service is temporarily unavailable");
    }

    @GetMapping("/users")
    public Mono<ResponseEntity<Map<String, Object>>> usersFallback() {
        log.warn("User service is unavailable, returning fallback response");
        return createFallbackResponse("User service is temporarily unavailable");
    }

    @GetMapping("/addresses")
    public Mono<ResponseEntity<Map<String, Object>>> addressesFallback() {
        log.warn("Address service is unavailable, returning fallback response");
        return createFallbackResponse("Address service is temporarily unavailable");
    }

    @GetMapping("/preferences")
    public Mono<ResponseEntity<Map<String, Object>>> preferencesFallback() {
        log.warn("Preferences service is unavailable, returning fallback response");
        return createFallbackResponse("Preferences service is temporarily unavailable");
    }

    @GetMapping("/favorites")
    public Mono<ResponseEntity<Map<String, Object>>> favoritesFallback() {
        log.warn("Favorites service is unavailable, returning fallback response");
        return createFallbackResponse("Favorites service is temporarily unavailable");
    }

    @GetMapping("/couriers")
    public Mono<ResponseEntity<Map<String, Object>>> couriersFallback() {
        log.warn("Courier service is unavailable, returning fallback response");
        return createFallbackResponse("Courier service is temporarily unavailable");
    }

    @GetMapping("/deliveries")
    public Mono<ResponseEntity<Map<String, Object>>> deliveriesFallback() {
        log.warn("Delivery service is unavailable, returning fallback response");
        return createFallbackResponse("Delivery service is temporarily unavailable");
    }

    private Mono<ResponseEntity<Map<String, Object>>> createFallbackResponse(String message) {
        Map<String, Object> response = Map.of(
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", message,
                "timestamp", Instant.now().toString()
        );

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}
