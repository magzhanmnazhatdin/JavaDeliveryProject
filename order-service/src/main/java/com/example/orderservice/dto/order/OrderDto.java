package com.example.orderservice.dto.order;

import com.example.orderservice.dto.payment.PaymentDto;
import com.example.orderservice.entity.OrderStatus;
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
public class OrderDto {

    private UUID id;
    private UUID customerId;
    private UUID restaurantId;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private String deliveryAddress;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;
    private String customerNotes;
    private String rejectionReason;
    private Instant estimatedDeliveryTime;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant confirmedAt;
    private Instant deliveredAt;
    private Instant cancelledAt;
    private List<OrderItemDto> items;
    private PaymentDto payment;
}
