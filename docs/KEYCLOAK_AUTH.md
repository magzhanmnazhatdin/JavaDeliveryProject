# Keycloak Authentication Documentation

## Overview

This project uses Keycloak as the Identity Provider (IdP) for OAuth2/OpenID Connect authentication. All microservices act as OAuth2 Resource Servers, validating JWT tokens issued by Keycloak.

---

## Keycloak Configuration

### Access Details

| Property | Value |
|----------|-------|
| Admin Console | http://localhost:8180 |
| Admin Username | `admin` |
| Admin Password | `admin` |
| Realm | `delivery-realm` |

### Realm Setup

```mermaid
flowchart TB
    subgraph Keycloak["Keycloak Server"]
        subgraph Realm["delivery-realm"]
            subgraph Clients["Clients"]
                C1[delivery-api<br/>Backend API Client]
                C2[delivery-web<br/>Web App Client]
            end

            subgraph Roles["Realm Roles"]
                R1[ADMIN]
                R2[CUSTOMER]
                R3[RESTAURANT]
                R4[COURIER]
            end

            subgraph Users["Users"]
                U1[admin-user]
                U2[customer-user]
                U3[restaurant-user]
                U4[courier-user]
            end
        end
    end

    U1 --> R1
    U2 --> R2
    U3 --> R3
    U4 --> R4
```

---

## Authentication Flow

### OAuth2 Password Grant Flow

```mermaid
sequenceDiagram
    participant U as User/Client
    participant KC as Keycloak
    participant GW as API Gateway
    participant MS as Microservice

    U->>KC: 1. POST /token<br/>(username, password, client_id)
    KC->>KC: 2. Validate Credentials
    KC-->>U: 3. Access Token (JWT) + Refresh Token

    U->>GW: 4. API Request<br/>Authorization: Bearer {token}
    GW->>KC: 5. Validate Token (JWKS)
    KC-->>GW: 6. Token Valid + Claims

    GW->>MS: 7. Forward Request + JWT
    MS->>MS: 8. Extract Roles from JWT
    MS->>MS: 9. Authorize Request
    MS-->>GW: 10. Response
    GW-->>U: 11. Response
```

### Token Refresh Flow

```mermaid
sequenceDiagram
    participant U as User/Client
    participant KC as Keycloak

    Note over U: Access Token Expired

    U->>KC: POST /token<br/>(grant_type=refresh_token,<br/>refresh_token={token})
    KC->>KC: Validate Refresh Token
    KC-->>U: New Access Token + Refresh Token
```

---

## Getting Access Token

### Option 1: Using cURL

```bash
# Get access token with password grant
curl -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "client_secret=your-client-secret" \
  -d "username=admin" \
  -d "password=admin123"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "scope": "profile email"
}
```

### Option 2: Using Postman

1. Create new request → Authorization tab
2. Select **OAuth 2.0**
3. Configure:
   ```
   Grant Type: Password Credentials
   Access Token URL: http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token
   Client ID: delivery-api
   Client Secret: <your-secret>
   Username: <user>
   Password: <password>
   Scope: openid profile email
   ```
4. Click **Get New Access Token**
5. Use token in requests

### Option 3: Using HTTPie

```bash
# Get token
http --form POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  grant_type=password \
  client_id=delivery-api \
  client_secret=your-secret \
  username=admin \
  password=admin123

# Use token
http GET http://localhost:8080/api/users/me \
  "Authorization: Bearer eyJhbGciOiJSUzI1NiIs..."
```

---

## JWT Token Structure

### Header
```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-id"
}
```

### Payload (Claims)
```json
{
  "exp": 1703520000,
  "iat": 1703519700,
  "jti": "unique-token-id",
  "iss": "http://localhost:8180/realms/delivery-realm",
  "sub": "user-uuid",
  "typ": "Bearer",
  "azp": "delivery-api",
  "preferred_username": "admin",
  "email": "admin@example.com",
  "realm_access": {
    "roles": ["ADMIN", "default-roles-delivery-realm"]
  },
  "resource_access": {
    "delivery-api": {
      "roles": ["admin"]
    }
  }
}
```

---

## Spring Security Configuration

### Resource Server Setup

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/restaurants/**").permitAll()

                // Role-based access
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasAnyRole("CUSTOMER", "ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
```

### Role Converter

```java
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null || realmAccess.isEmpty()) {
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            .collect(Collectors.toList());
    }
}
```

### Application Properties

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/delivery-realm
          jwk-set-uri: http://localhost:8180/realms/delivery-realm/protocol/openid-connect/certs
```

---

## Role-Based Access Control (RBAC)

### Roles and Permissions

```mermaid
flowchart TB
    subgraph Roles
        ADMIN[ADMIN<br/>Full System Access]
        CUSTOMER[CUSTOMER<br/>Order & Profile]
        RESTAURANT[RESTAURANT<br/>Menu & Orders]
        COURIER[COURIER<br/>Deliveries]
    end

    subgraph OrderService["Order Service Endpoints"]
        O1[POST /api/orders]
        O2[GET /api/orders/my-orders]
        O3[GET /api/orders/status/*]
        O4[POST /api/payments/*/refund]
    end

    subgraph UserService["User Service Endpoints"]
        U1[GET /api/users/me]
        U2[GET /api/users]
        U3[DELETE /api/users/*]
    end

    subgraph RestaurantService["Restaurant Service Endpoints"]
        R1[GET /api/restaurants]
        R2[POST /api/restaurants]
        R3[POST /api/restaurants/*/menu]
    end

    subgraph DeliveryService["Delivery Service Endpoints"]
        D1[PATCH /api/couriers/*/location]
        D2[GET /api/couriers]
        D3[POST /api/deliveries/*/assign]
    end

    ADMIN --> O1 & O2 & O3 & O4
    ADMIN --> U1 & U2 & U3
    ADMIN --> R1 & R2 & R3
    ADMIN --> D1 & D2 & D3

    CUSTOMER --> O1 & O2
    CUSTOMER --> U1

    RESTAURANT --> R1 & R2 & R3

    COURIER --> D1
```

### Endpoint Security Matrix

| Endpoint | ADMIN | CUSTOMER | RESTAURANT | COURIER | Public |
|----------|-------|----------|------------|---------|--------|
| `GET /api/restaurants` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `POST /api/orders` | ✅ | ✅ | ❌ | ❌ | ❌ |
| `GET /api/users/me` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `GET /api/users` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `POST /api/restaurants` | ✅ | ❌ | ✅ | ❌ | ❌ |
| `PATCH /api/couriers/{id}/location` | ✅ | ❌ | ❌ | ✅ | ❌ |
| `POST /api/deliveries/{id}/assign` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `/swagger-ui/**` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `/actuator/health` | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## Method-Level Security

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDto> getCurrentUser() {
        // Available to any authenticated user
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        // Only ADMIN
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        // Only ADMIN
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<UserDto> updateProfile(@RequestBody UpdateUserRequest request) {
        // CUSTOMER or ADMIN
    }
}
```

---

## Accessing User Information

### Getting Current User ID from JWT

```java
@Service
public class UserService {

    public String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();
            return jwt.getSubject(); // Keycloak user ID
        }

        throw new UnauthorizedException("User not authenticated");
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();
            return jwt.getClaimAsString("preferred_username");
        }

        return null;
    }
}
```

### Controller Example

```java
@GetMapping("/me")
public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    String keycloakId = jwt.getSubject();

    User user = userService.findByKeycloakId(keycloakId)
        .orElseThrow(() -> new UserNotFoundException("User not found"));

    return ResponseEntity.ok(userMapper.toDto(user));
}
```

---

## Testing with Keycloak

### Integration Test Setup

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getCurrentUser_shouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_shouldReturnList() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## Keycloak Setup Steps

### 1. Create Realm

1. Login to Keycloak Admin Console
2. Hover over "Master" dropdown → "Create Realm"
3. Name: `delivery-realm`
4. Click "Create"

### 2. Create Client

1. Go to Clients → Create client
2. Client ID: `delivery-api`
3. Client Protocol: `openid-connect`
4. Access Type: `confidential`
5. Enable "Service Accounts Enabled"
6. Valid Redirect URIs: `http://localhost:*`
7. Save and copy Client Secret

### 3. Create Roles

1. Go to Realm Roles → Create role
2. Create: `ADMIN`, `CUSTOMER`, `RESTAURANT`, `COURIER`

### 4. Create Users

1. Go to Users → Create user
2. Set username, email
3. Credentials tab → Set password
4. Role Mappings → Assign roles

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Check token expiration, validate token format |
| 403 Forbidden | Verify user has required role |
| Token validation failed | Check issuer-uri matches Keycloak URL |
| Roles not recognized | Verify KeycloakRealmRoleConverter is configured |

### Debug Logging

```yaml
logging:
  level:
    org.springframework.security: DEBUG
    org.springframework.security.oauth2: DEBUG
```
