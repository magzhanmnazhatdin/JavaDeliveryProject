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
public class UserUpdatedEvent {

    @Builder.Default
    private String eventType = "USER_UPDATED";
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private Instant updatedAt;
}
