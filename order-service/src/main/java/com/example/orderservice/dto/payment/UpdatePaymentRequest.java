package com.example.orderservice.dto.payment;

import com.example.orderservice.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaymentRequest {

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
