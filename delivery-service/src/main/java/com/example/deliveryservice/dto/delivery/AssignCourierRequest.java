package com.example.deliveryservice.dto.delivery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to manually assign a courier to a delivery")
public class AssignCourierRequest {

    @NotNull(message = "Courier ID is required")
    @Schema(description = "ID of the courier to assign")
    private UUID courierId;
}
