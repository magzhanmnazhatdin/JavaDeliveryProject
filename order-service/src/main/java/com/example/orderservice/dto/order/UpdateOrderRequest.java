package com.example.orderservice.dto.order;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {

    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    private String deliveryAddress;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal deliveryLat;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal deliveryLng;

    @Size(max = 1000, message = "Customer notes must not exceed 1000 characters")
    private String customerNotes;
}
