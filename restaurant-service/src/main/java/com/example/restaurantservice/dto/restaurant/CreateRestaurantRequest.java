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
@Schema(description = "Request to create a new restaurant")
public class CreateRestaurantRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Restaurant name", example = "Pizza Palace")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    @Schema(description = "Restaurant description", example = "Best pizza in town")
    private String description;

    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must be less than 500 characters")
    @Schema(description = "Restaurant address", example = "123 Main St")
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be less than 100 characters")
    @Schema(description = "City", example = "New York")
    private String city;

    @Size(max = 50, message = "Phone must be less than 50 characters")
    @Schema(description = "Phone number", example = "+1234567890")
    private String phone;

    @Email(message = "Invalid email format")
    @Schema(description = "Email address", example = "contact@pizzapalace.com")
    private String email;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    @Schema(description = "Latitude", example = "40.7128")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    @Schema(description = "Longitude", example = "-74.0060")
    private BigDecimal longitude;

    @Schema(description = "Opening time", example = "09:00")
    private LocalTime openingTime;

    @Schema(description = "Closing time", example = "22:00")
    private LocalTime closingTime;
}
