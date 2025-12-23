package com.example.deliveryservice.dto.event;

import com.example.deliveryservice.entity.DeliveryStatus;
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
    private DeliveryStatus previousStatus;
    private DeliveryStatus newStatus;
    private String notes;
    private Instant changedAt;
}
