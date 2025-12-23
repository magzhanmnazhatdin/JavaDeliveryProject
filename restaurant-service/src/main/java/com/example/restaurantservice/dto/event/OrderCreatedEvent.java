package com.example.restaurantservice.dto.event;

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
public class OrderCreatedEvent {
    private String eventType;
    private UUID orderId;
    private UUID customerId;
    private UUID restaurantId;
    private BigDecimal totalPrice;
    private String deliveryAddress;
    private String customerNotes;
    private List<OrderItemEvent> items;
    private Instant createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemEvent {
        private UUID menuItemId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
        private String specialInstructions;
    }
}
