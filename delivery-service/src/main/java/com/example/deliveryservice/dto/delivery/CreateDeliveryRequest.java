package com.example.deliveryservice.dto.delivery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Request to create a new delivery")
public class CreateDeliveryRequest {

    @NotNull(message = "Order ID is required")
    @Schema(description = "ID of the order", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID orderId;

    @NotNull(message = "Customer ID is required")
    @Schema(description = "ID of the customer")
    private UUID customerId;

    @NotNull(message = "Restaurant ID is required")
    @Schema(description = "ID of the restaurant")
    private UUID restaurantId;

    @NotBlank(message = "Delivery address is required")
    @Size(max = 500, message = "Address must be less than 500 characters")
    @Schema(description = "Delivery address", example = "123 Main St, New York, NY 10001")
    private String deliveryAddress;

    @Schema(description = "Delivery latitude", example = "40.7128")
    private BigDecimal deliveryLat;

    @Schema(description = "Delivery longitude", example = "-74.0060")
    private BigDecimal deliveryLng;

    @Size(max = 500, message = "Pickup address must be less than 500 characters")
    @Schema(description = "Restaurant pickup address")
    private String pickupAddress;

    @Schema(description = "Pickup latitude")
    private BigDecimal pickupLat;

    @Schema(description = "Pickup longitude")
    private BigDecimal pickupLng;

    @Size(max = 500, message = "Customer notes must be less than 500 characters")
    @Schema(description = "Special delivery instructions from customer", example = "Leave at door")
    private String customerNotes;
}
