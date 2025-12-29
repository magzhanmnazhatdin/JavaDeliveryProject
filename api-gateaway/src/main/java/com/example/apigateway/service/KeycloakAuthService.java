package com.example.apigateway.service;

import com.example.apigateway.dto.AuthResponse;
import com.example.apigateway.dto.LoginRequest;
import com.example.apigateway.dto.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

            String role = "CUSTOMER";
            if (claims.has("realm_access")) {
                JsonNode realmAccess = claims.get("realm_access");
                if (realmAccess.has("roles")) {
                    for (JsonNode r : realmAccess.get("roles")) {
                        String roleName = r.asText();
                        if (List.of("ADMIN", "RESTAURANT_OWNER", "COURIER", "CUSTOMER").contains(roleName)) {
                            role = roleName;
                            break;
                        }
                    }
                }
            }

            return AuthResponse.UserInfo.builder()
                    .id(claims.has("sub") ? claims.get("sub").asText() : null)
                    .email(claims.has("email") ? claims.get("email").asText() : null)
                    .firstName(claims.has("given_name") ? claims.get("given_name").asText() : null)
                    .lastName(claims.has("family_name") ? claims.get("family_name").asText() : null)
                    .role(role)
                    .build();
        } catch (Exception e) {
            log.error("Failed to extract user info from token", e);
            return AuthResponse.UserInfo.builder().build();
        }
    }
}
