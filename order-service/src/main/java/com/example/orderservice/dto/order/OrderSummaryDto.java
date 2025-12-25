package com.example.orderservice.dto.order;

import com.example.orderservice.entity.OrderStatus;
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
public class OrderSummaryDto {

    private UUID id;
    private UUID restaurantId;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private String deliveryAddress;
    private Integer itemCount;
    private Instant createdAt;
    private Instant estimatedDeliveryTime;
}
