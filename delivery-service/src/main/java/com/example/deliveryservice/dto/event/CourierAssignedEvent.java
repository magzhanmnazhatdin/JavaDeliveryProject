package com.example.deliveryservice.dto.event;

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
public class CourierAssignedEvent {
    private String eventType;
    private UUID deliveryId;
    private UUID orderId;
    private UUID courierId;
    private String courierName;
    private String courierPhone;
    private Instant assignedAt;
}
