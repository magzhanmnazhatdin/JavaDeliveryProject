package com.example.deliveryservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Admin only endpoints
                        .requestMatchers(HttpMethod.POST, "/api/couriers").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/couriers/**").hasRole("ADMIN")

                        // Courier endpoints - courier or admin
                        .requestMatchers(HttpMethod.GET, "/api/couriers/me").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/couriers/me").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/couriers/me/toggle-availability").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/couriers/me/location").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/couriers/*/status").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/couriers/*/location").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/deliveries/*/status").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries/available").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries/courier/me").hasAnyRole("COURIER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/deliveries/*/accept").hasAnyRole("COURIER", "ADMIN")

                        // Customer endpoints
                        .requestMatchers(HttpMethod.GET, "/api/deliveries/customer/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/deliveries/order/**").hasAnyRole("CUSTOMER", "RESTAURANT", "ADMIN")

                        // Restaurant endpoints
                        .requestMatchers(HttpMethod.GET, "/api/deliveries/status/**").hasAnyRole("RESTAURANT", "ADMIN")

                        // All other endpoints require authentication
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

    static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null || realmAccess.isEmpty()) {
                return List.of();
            }

            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) {
                return List.of();
            }

            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());
        }
    }
}
