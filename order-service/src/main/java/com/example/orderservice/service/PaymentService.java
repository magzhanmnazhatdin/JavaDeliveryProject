package com.example.orderservice.service;

import com.example.orderservice.dto.payment.PaymentDto;
import com.example.orderservice.dto.payment.ProcessPaymentRequest;
import com.example.orderservice.dto.payment.RefundRequest;
import com.example.orderservice.dto.payment.UpdatePaymentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {

    PaymentDto processPayment(ProcessPaymentRequest request);

    PaymentDto getPaymentById(UUID paymentId);

    PaymentDto getPaymentByOrderId(UUID orderId);

    Page<PaymentDto> getPaymentsByCustomer(UUID customerId, Pageable pageable);

    PaymentDto updatePayment(UUID paymentId, UpdatePaymentRequest request);

    PaymentDto refundPayment(UUID paymentId, RefundRequest request);

    PaymentDto cancelPayment(UUID paymentId);
}
