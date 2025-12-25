package com.example.deliveryservice.dto.delivery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update delivery details")
public class UpdateDeliveryRequest {

    @Size(max = 500, message = "Address must be less than 500 characters")
    @Schema(description = "Updated delivery address", example = "456 New St, New York, NY 10002")
    private String deliveryAddress;

    @Schema(description = "Updated delivery latitude", example = "40.7130")
    private BigDecimal deliveryLat;

    @Schema(description = "Updated delivery longitude", example = "-74.0055")
    private BigDecimal deliveryLng;

    @Size(max = 500, message = "Customer notes must be less than 500 characters")
    @Schema(description = "Updated delivery instructions", example = "Ring doorbell twice")
    private String customerNotes;
}
