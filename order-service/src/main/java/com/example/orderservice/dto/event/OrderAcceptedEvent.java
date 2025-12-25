package com.example.orderservice.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAcceptedEvent {

    private String eventType;
    private UUID orderId;
    private UUID customerId;
    private UUID restaurantId;
    private String restaurantName;
    private String restaurantAddress;
    private BigDecimal restaurantLat;
    private BigDecimal restaurantLng;
    private String deliveryAddress;
    private BigDecimal totalPrice;
    private Integer estimatedPrepTimeMinutes;
    private String customerNotes;
    private Instant acceptedAt;
}
