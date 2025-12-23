package com.example.restaurantservice.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Request to create a new menu item")
public class CreateMenuItemRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Menu item name", example = "Margherita Pizza")
    private String name;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    @Schema(description = "Item description", example = "Classic pizza with tomato sauce and mozzarella")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid")
    @Schema(description = "Price", example = "12.99")
    private BigDecimal price;

    @Size(max = 100, message = "Category must be less than 100 characters")
    @Schema(description = "Category", example = "Pizza")
    private String category;

    @Size(max = 500, message = "Image URL must be less than 500 characters")
    @Schema(description = "Image URL")
    private String imageUrl;

    @Min(value = 1, message = "Preparation time must be at least 1 minute")
    @Max(value = 180, message = "Preparation time must be less than 180 minutes")
    @Schema(description = "Preparation time in minutes", example = "20")
    private Integer preparationTimeMinutes;
}
