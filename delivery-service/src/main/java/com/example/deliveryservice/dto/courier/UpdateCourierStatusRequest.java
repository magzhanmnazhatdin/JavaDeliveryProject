package com.example.deliveryservice.dto.courier;

import com.example.deliveryservice.entity.CourierStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update courier status")
public class UpdateCourierStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New courier status", example = "AVAILABLE")
    private CourierStatus status;
}
