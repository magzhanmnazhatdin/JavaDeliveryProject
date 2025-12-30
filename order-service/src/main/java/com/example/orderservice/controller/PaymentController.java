package com.example.orderservice.controller;

import com.example.orderservice.dto.payment.PaymentDto;
import com.example.orderservice.dto.payment.ProcessPaymentRequest;
import com.example.orderservice.dto.payment.RefundRequest;
import com.example.orderservice.dto.payment.UpdatePaymentRequest;
import com.example.orderservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management API")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Process a payment for an order")
    public ResponseEntity<PaymentDto> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<PaymentDto> getPaymentByOrderId(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/my-payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's payments")
    public ResponseEntity<Page<PaymentDto>> getMyPayments(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(paymentService.getPaymentsByCustomer(customerId, pageable));
    }

    @PutMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update payment method (only for pending payments)")
    public ResponseEntity<PaymentDto> updatePayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.updatePayment(paymentId, request));
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund a payment (Admin only)")
    public ResponseEntity<PaymentDto> refundPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request
    ) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId, request));
    }

    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel a pending payment (Admin only)")
    public ResponseEntity<PaymentDto> cancelPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId));
    }
}
