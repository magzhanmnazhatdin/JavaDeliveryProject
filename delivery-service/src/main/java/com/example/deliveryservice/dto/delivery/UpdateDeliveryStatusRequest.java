package com.example.deliveryservice.dto.delivery;

import com.example.deliveryservice.entity.DeliveryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to update delivery status")
public class UpdateDeliveryStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New delivery status", example = "PICKED_UP")
    private DeliveryStatus status;

    @Size(max = 500, message = "Notes must be less than 500 characters")
    @Schema(description = "Courier notes about the delivery", example = "Customer not home, left with neighbor")
    private String courierNotes;

    @Size(max = 500, message = "Cancellation reason must be less than 500 characters")
    @Schema(description = "Reason for cancellation (required if status is CANCELLED)")
    private String cancellationReason;
}
