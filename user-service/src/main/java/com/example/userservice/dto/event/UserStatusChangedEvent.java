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
public class UserStatusChangedEvent {

    @Builder.Default
    private String eventType = "USER_STATUS_CHANGED";
    private UUID userId;
    private String previousStatus;
    private String newStatus;
    private Instant changedAt;
}
