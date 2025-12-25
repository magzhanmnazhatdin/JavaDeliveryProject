package com.example.userservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {

    @Builder.Default
    private String eventType = "USER_CREATED";
    private UUID userId;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private Instant createdAt;
}
