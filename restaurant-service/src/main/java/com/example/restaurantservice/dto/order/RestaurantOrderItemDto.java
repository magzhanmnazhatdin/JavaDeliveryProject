package com.example.restaurantservice.dto.order;

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
public class RestaurantOrderItemDto {
    private UUID id;
    private UUID menuItemId;
    private String nameSnapshot;
    private BigDecimal priceSnapshot;
    private Integer quantity;
    private String specialInstructions;
}
