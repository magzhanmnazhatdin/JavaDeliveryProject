package com.example.restaurantservice.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to accept an order")
public class AcceptOrderRequest {

    @Min(value = 5, message = "Estimated preparation time must be at least 5 minutes")
    @Max(value = 180, message = "Estimated preparation time must be less than 180 minutes")
    @Schema(description = "Estimated preparation time in minutes", example = "30")
    private Integer estimatedPrepTimeMinutes;
}
