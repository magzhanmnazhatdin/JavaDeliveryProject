package com.example.restaurantservice.dto.event;

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
public class OrderRejectedEvent {
    private String eventType;
    private UUID orderId;
    private UUID restaurantId;
    private String rejectionReason;
    private Instant rejectedAt;
}
