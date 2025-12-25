package com.example.orderservice.dto.event;

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
public class OrderCancelledEvent {

    @Builder.Default
    private String eventType = "ORDER_CANCELLED";
    private UUID orderId;
    private UUID customerId;
    private UUID restaurantId;
    private String cancellationReason;
    private Instant cancelledAt;
}
