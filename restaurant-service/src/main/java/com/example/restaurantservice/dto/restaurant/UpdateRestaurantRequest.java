package com.example.restaurantservice.dto.restaurant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update restaurant information")
public class UpdateRestaurantRequest {

    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Restaurant name")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    @Schema(description = "Restaurant description")
    private String description;

    @Size(max = 500, message = "Address must be less than 500 characters")
    @Schema(description = "Restaurant address")
    private String address;

    @Size(max = 100, message = "City must be less than 100 characters")
    @Schema(description = "City")
    private String city;

    @Size(max = 50, message = "Phone must be less than 50 characters")
    @Schema(description = "Phone number")
    private String phone;

    @Email(message = "Invalid email format")
    @Schema(description = "Email address")
    private String email;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Schema(description = "Latitude")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Schema(description = "Longitude")
    private BigDecimal longitude;

    @Schema(description = "Opening time")
    private LocalTime openingTime;

    @Schema(description = "Closing time")
    private LocalTime closingTime;

    @Schema(description = "Is restaurant active")
    private Boolean isActive;
}
