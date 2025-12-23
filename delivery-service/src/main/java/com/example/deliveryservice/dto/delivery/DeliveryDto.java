package com.example.deliveryservice.dto.delivery;

import com.example.deliveryservice.dto.courier.CourierDto;
import com.example.deliveryservice.entity.DeliveryStatus;
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
public class DeliveryDto {
    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private UUID restaurantId;
    private CourierDto courier;

    private String deliveryAddress;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;

    private String pickupAddress;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;

    private DeliveryStatus status;

    private Instant assignedAt;
    private Instant pickedUpAt;
    private Instant deliveredAt;
    private Instant cancelledAt;

    private String cancellationReason;
    private String customerNotes;
    private String courierNotes;

    private Instant createdAt;
    private Instant updatedAt;
}
