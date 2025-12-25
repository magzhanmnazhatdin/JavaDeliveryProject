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
public class DeliveryStatusChangedEvent {

    private String eventType;
    private UUID deliveryId;
    private UUID orderId;
    private UUID courierId;
    private String previousStatus;
    private String newStatus;
    private String notes;
    private Instant changedAt;
}
