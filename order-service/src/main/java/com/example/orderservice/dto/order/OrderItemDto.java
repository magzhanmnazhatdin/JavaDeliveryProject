package com.example.orderservice.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {

    private UUID id;
    private UUID menuItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
    private String specialInstructions;
}
