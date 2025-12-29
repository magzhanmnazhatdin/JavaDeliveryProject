package com.example.apigateway.controller;

import com.example.apigateway.dto.AuthResponse;
import com.example.apigateway.dto.LoginRequest;
import com.example.apigateway.dto.RegisterRequest;
import com.example.apigateway.service.KeycloakAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService authService;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());
        return authService.login(request)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Login failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(null));
                });
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Object>> register(@RequestBody RegisterRequest request) {
        log.info("Registration attempt for user: {}", request.getEmail());
        return authService.register(request)
                .<Object>map(response -> response)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response))
                .onErrorResume(e -> {
                    log.error("Registration failed: {}", e.getMessage());
                    HttpStatus status = e.getMessage().contains("already exists")
                            ? HttpStatus.CONFLICT
                            : HttpStatus.BAD_REQUEST;
                    return Mono.just(ResponseEntity
                            .status(status)
                            .body(Map.of("message", e.getMessage())));
                });
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        return authService.refreshToken(refreshToken)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Token refresh failed: {}", e.getMessage());
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.UNAUTHORIZED)
                            .body(null));
                });
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Map<String, String>>> logout() {
        // Client-side logout - just return success
        // The client should clear the tokens
        return Mono.just(ResponseEntity.ok(Map.of("message", "Logged out successfully")));
    }
}
