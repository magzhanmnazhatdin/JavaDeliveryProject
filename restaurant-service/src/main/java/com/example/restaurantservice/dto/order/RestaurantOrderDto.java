package com.example.restaurantservice.dto.order;

import com.example.restaurantservice.entity.RestaurantOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantOrderDto {
    private UUID id;
    private UUID orderId;
    private UUID restaurantId;
    private UUID customerId;
    private BigDecimal totalPrice;
    private RestaurantOrderStatus status;
    private String deliveryAddress;
    private String customerNotes;
    private Instant receivedAt;
    private Instant acceptedAt;
    private Instant rejectedAt;
    private Instant preparingAt;
    private Instant readyAt;
    private String rejectionReason;
    private Integer estimatedPrepTimeMinutes;
    private List<RestaurantOrderItemDto> items;
    private Instant createdAt;
    private Instant updatedAt;
}
