package com.example.deliveryservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Delivery Service API",
                version = "1.0",
                description = "API for managing deliveries and couriers in the food delivery system",
                contact = @Contact(
                        name = "Delivery Team",
                        email = "delivery@example.com"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8084", description = "Local development server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT token from Keycloak"
)
public class OpenApiConfig {
}
