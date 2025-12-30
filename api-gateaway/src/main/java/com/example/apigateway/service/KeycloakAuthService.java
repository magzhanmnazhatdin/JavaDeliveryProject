package com.example.apigateway.service;

import com.example.apigateway.dto.AuthResponse;
import com.example.apigateway.dto.BecomeCourierRequest;
import com.example.apigateway.dto.BecomeRestaurantRequest;
import com.example.apigateway.dto.LoginRequest;
import com.example.apigateway.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakAuthService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.auth-server-url:http://localhost:8180}")
    private String keycloakUrl;

    @Value("${keycloak.realm:delivery-realm}")
    private String realm;

    @Value("${keycloak.client-id:delivery-app}")
    private String clientId;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    @Value("${services.delivery-service.url:http://localhost:8084}")
    private String deliveryServiceUrl;

    @Value("${services.restaurant-service.url:http://localhost:8082}")
    private String restaurantServiceUrl;

    public KeycloakAuthService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", clientId)
                        .with("username", request.getEmail())
                        .with("password", request.getPassword()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::mapTokenResponse)
                .onErrorResume(e -> {
                    log.error("Login failed: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Invalid email or password"));
                });
    }

    public Mono<AuthResponse> register(RegisterRequest request) {
        return getAdminToken()
                .flatMap(adminToken -> createUser(adminToken, request))
                .flatMap(userId -> {
                    // After creating user, log them in to get tokens
                    LoginRequest loginRequest = new LoginRequest();
                    loginRequest.setEmail(request.getEmail());
                    loginRequest.setPassword(request.getPassword());
                    return login(loginRequest);
                })
                .onErrorResume(e -> {
                    log.error("Registration failed: {}", e.getMessage());
                    if (e.getMessage().contains("409")) {
                        return Mono.error(new RuntimeException("User with this email already exists"));
                    }
                    return Mono.error(new RuntimeException("Registration failed: " + e.getMessage()));
                });
    }

    public Mono<AuthResponse> refreshToken(String refreshToken) {
        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", clientId)
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::mapTokenResponse)
                .onErrorResume(e -> {
                    log.error("Token refresh failed: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Invalid refresh token"));
                });
    }

    private Mono<String> getAdminToken() {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "password")
                        .with("client_id", "admin-cli")
                        .with("username", adminUsername)
                        .with("password", adminPassword))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.get("access_token").asText());
    }

    private Mono<String> createUser(String adminToken, RegisterRequest request) {
        String usersUrl = keycloakUrl + "/admin/realms/" + realm + "/users";

        Map<String, Object> userRepresentation = Map.of(
                "username", request.getEmail(),
                "email", request.getEmail(),
                "firstName", request.getFirstName(),
                "lastName", request.getLastName(),
                "enabled", true,
                "emailVerified", true,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", request.getPassword(),
                        "temporary", false
                )),
                "realmRoles", List.of("CUSTOMER"),
                "attributes", Map.of(
                        "phone", List.of(request.getPhone() != null ? request.getPhone() : "")
                )
        );

        return webClient.post()
                .uri(usersUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userRepresentation)
                .retrieve()
                .toBodilessEntity()
                .flatMap(response -> {
                    String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
                    if (location != null) {
                        String userId = location.substring(location.lastIndexOf('/') + 1);
                        // Assign CUSTOMER role to the user
                        return assignRole(adminToken, userId, "CUSTOMER")
                                .thenReturn(userId);
                    }
                    return Mono.error(new RuntimeException("Failed to create user"));
                });
    }

    private Mono<Void> assignRole(String adminToken, String userId, String roleName) {
        // First get the role
        String rolesUrl = keycloakUrl + "/admin/realms/" + realm + "/roles/" + roleName;

        return webClient.get()
                .uri(rolesUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(role -> {
                    String assignUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
                    List<Map<String, Object>> roles = List.of(Map.of(
                            "id", role.get("id").asText(),
                            "name", role.get("name").asText()
                    ));

                    return webClient.post()
                            .uri(assignUrl)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(roles)
                            .retrieve()
                            .toBodilessEntity()
                            .then();
                });
    }

    private AuthResponse mapTokenResponse(JsonNode json) {
        String accessToken = json.get("access_token").asText();
        String refreshToken = json.has("refresh_token") ? json.get("refresh_token").asText() : null;
        Long expiresIn = json.get("expires_in").asLong();

        // Decode JWT to get user info
        AuthResponse.UserInfo userInfo = extractUserInfo(accessToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(userInfo)
                .build();
    }

    private AuthResponse.UserInfo extractUserInfo(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            List<String> userRoles = new ArrayList<>();
            String primaryRole = "CUSTOMER";

            if (claims.has("realm_access")) {
                JsonNode realmAccess = claims.get("realm_access");
                if (realmAccess.has("roles")) {
                    for (JsonNode r : realmAccess.get("roles")) {
                        String roleName = r.asText();
                        if (List.of("ADMIN", "RESTAURANT_OWNER", "COURIER", "CUSTOMER").contains(roleName)) {
                            userRoles.add(roleName);
                        }
                    }
                    // Set primary role (priority: ADMIN > RESTAURANT_OWNER > COURIER > CUSTOMER)
                    if (userRoles.contains("ADMIN")) {
                        primaryRole = "ADMIN";
                    } else if (userRoles.contains("RESTAURANT_OWNER")) {
                        primaryRole = "RESTAURANT_OWNER";
                    } else if (userRoles.contains("COURIER")) {
                        primaryRole = "COURIER";
                    }
                }
            }

            if (userRoles.isEmpty()) {
                userRoles.add("CUSTOMER");
            }

            return AuthResponse.UserInfo.builder()
                    .id(claims.has("sub") ? claims.get("sub").asText() : null)
                    .email(claims.has("email") ? claims.get("email").asText() : null)
                    .firstName(claims.has("given_name") ? claims.get("given_name").asText() : null)
                    .lastName(claims.has("family_name") ? claims.get("family_name").asText() : null)
                    .role(primaryRole)
                    .roles(userRoles)
                    .build();
        } catch (Exception e) {
            log.error("Failed to extract user info from token", e);
            return AuthResponse.UserInfo.builder().build();
        }
    }

    /**
     * Become a courier - adds COURIER role and creates courier profile
     */
    public Mono<AuthResponse> becomeCourier(BecomeCourierRequest request, String accessToken, String refreshToken) {
        String userId = extractUserIdFromToken(accessToken);
        if (userId == null) {
            return Mono.error(new RuntimeException("Invalid access token"));
        }

        return getAdminToken()
                .flatMap(adminToken ->
                    // Check if user already has COURIER role
                    getUserRoles(adminToken, userId)
                        .flatMap(roles -> {
                            if (roles.contains("COURIER")) {
                                return Mono.error(new RuntimeException("User is already a courier"));
                            }
                            // Add COURIER role to user
                            return assignRole(adminToken, userId, "COURIER");
                        })
                )
                .then(createCourierProfile(request, userId, accessToken))
                .then(refreshToken != null && !refreshToken.isBlank()
                        ? refreshToken(refreshToken)
                        : refreshTokenFromAccessToken(accessToken))
                .onErrorResume(e -> {
                    log.error("Failed to become courier: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Failed to become courier: " + e.getMessage()));
                });
    }

    /**
     * Become a restaurant owner - adds RESTAURANT_OWNER role and creates restaurant
     */
    public Mono<AuthResponse> becomeRestaurant(BecomeRestaurantRequest request, String accessToken, String refreshToken) {
        String userId = extractUserIdFromToken(accessToken);
        if (userId == null) {
            return Mono.error(new RuntimeException("Invalid access token"));
        }

        return getAdminToken()
                .flatMap(adminToken ->
                    // Check if user already has RESTAURANT_OWNER role
                    getUserRoles(adminToken, userId)
                        .flatMap(roles -> {
                            if (roles.contains("RESTAURANT_OWNER")) {
                                return Mono.error(new RuntimeException("User already has a restaurant"));
                            }
                            // Add RESTAURANT_OWNER role to user
                            return assignRole(adminToken, userId, "RESTAURANT_OWNER");
                        })
                )
                .then(createRestaurantProfile(request, userId, accessToken))
                .then(refreshToken != null && !refreshToken.isBlank()
                        ? refreshToken(refreshToken)
                        : refreshTokenFromAccessToken(accessToken))
                .onErrorResume(e -> {
                    log.error("Failed to become restaurant owner: {}", e.getMessage());
                    return Mono.error(new RuntimeException("Failed to become restaurant owner: " + e.getMessage()));
                });
    }

    private String extractUserIdFromToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);
            return claims.has("sub") ? claims.get("sub").asText() : null;
        } catch (Exception e) {
            log.error("Failed to extract user ID from token", e);
            return null;
        }
    }

    private Mono<List<String>> getUserRoles(String adminToken, String userId) {
        String rolesUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        return webClient.get()
                .uri(rolesUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> {
                    List<String> roles = new ArrayList<>();
                    if (json.isArray()) {
                        for (JsonNode role : json) {
                            roles.add(role.get("name").asText());
                        }
                    }
                    return roles;
                })
                .onErrorReturn(new ArrayList<>());
    }

    private Mono<Void> createCourierProfile(BecomeCourierRequest request, String userId, String accessToken) {
        String courierUrl = deliveryServiceUrl + "/api/couriers";

        Map<String, Object> courierData = Map.of(
                "name", request.getName(),
                "phone", request.getPhone(),
                "email", request.getEmail() != null ? request.getEmail() : "",
                "keycloakId", userId
        );

        return webClient.post()
                .uri(courierUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(courierData)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        log.warn("Courier profile already exists for user {}", userId);
                        return Mono.empty();
                    }
                    return Mono.error(e);
                });
    }

    private Mono<Void> createRestaurantProfile(BecomeRestaurantRequest request, String userId, String accessToken) {
        String restaurantUrl = restaurantServiceUrl + "/api/restaurants";

        Map<String, Object> restaurantData = new java.util.HashMap<>();
        restaurantData.put("name", request.getName());
        restaurantData.put("address", request.getAddress());
        restaurantData.put("city", request.getCity());
        restaurantData.put("keycloakId", userId);

        if (request.getDescription() != null) {
            restaurantData.put("description", request.getDescription());
        }
        if (request.getPhone() != null) {
            restaurantData.put("phone", request.getPhone());
        }
        if (request.getEmail() != null) {
            restaurantData.put("email", request.getEmail());
        }
        if (request.getLatitude() != null) {
            restaurantData.put("latitude", request.getLatitude());
        }
        if (request.getLongitude() != null) {
            restaurantData.put("longitude", request.getLongitude());
        }
        if (request.getOpeningTime() != null) {
            restaurantData.put("openingTime", request.getOpeningTime().toString());
        }
        if (request.getClosingTime() != null) {
            restaurantData.put("closingTime", request.getClosingTime().toString());
        }

        return webClient.post()
                .uri(restaurantUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(restaurantData)
                .retrieve()
                .toBodilessEntity()
                .then()
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        log.warn("Restaurant already exists for user {}", userId);
                        return Mono.empty();
                    }
                    return Mono.error(e);
                });
    }

    private Mono<AuthResponse> refreshTokenFromAccessToken(String accessToken) {
        // We need to get a fresh token with the new role
        // Since we have the access token, we'll use the token introspection to get refresh token
        // For simplicity, we'll return the same token with updated user info
        // The client should call refresh to get updated roles

        String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        // Extract the session from current token and use refresh
        try {
            String[] parts = accessToken.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            // We need to force a token refresh by logging in again or using refresh token
            // Since we don't have the refresh token here, we'll return the current token
            // with a flag indicating roles have changed

            AuthResponse.UserInfo userInfo = extractUserInfo(accessToken);

            return Mono.just(AuthResponse.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .user(userInfo)
                    .build());
        } catch (Exception e) {
            log.error("Failed to refresh token", e);
            return Mono.error(new RuntimeException("Failed to refresh token"));
        }
    }
}
