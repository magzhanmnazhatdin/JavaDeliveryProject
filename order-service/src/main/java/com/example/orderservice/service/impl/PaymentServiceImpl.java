package com.example.orderservice.service.impl;

import com.example.orderservice.dto.payment.PaymentDto;
import com.example.orderservice.dto.payment.ProcessPaymentRequest;
import com.example.orderservice.dto.payment.RefundRequest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.PaymentStatus;
import com.example.orderservice.exception.InvalidPaymentStateException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.PaymentNotFoundException;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.mapper.PaymentMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final OrderEventProducer orderEventProducer;

    @Override
    public PaymentDto processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + request.getOrderId()));

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new PaymentNotFoundException("No payment found for order: " + request.getOrderId());
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(
                    "Payment cannot be processed in status: " + payment.getStatus()
            );
        }

        boolean paymentSuccess = simulatePaymentProcessing(request);

        if (paymentSuccess) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId(generateTransactionId());
            payment.setPaidAt(Instant.now());

            order.setStatus(OrderStatus.CONFIRMED);
            order.setConfirmedAt(Instant.now());

            log.info("Payment completed successfully for order: {}", request.getOrderId());

            orderEventProducer.sendPaymentCompletedEvent(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment processing failed");

            log.warn("Payment failed for order: {}", request.getOrderId());

            orderEventProducer.sendPaymentFailedEvent(payment, "Payment processing failed");
        }

        paymentRepository.save(payment);
        orderRepository.save(order);

        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));
        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDto getPaymentByOrderId(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order: " + orderId));
        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDto> getPaymentsByCustomer(UUID customerId, Pageable pageable) {
        return paymentRepository.findByCustomerId(customerId, pageable)
                .map(paymentMapper::toDto);
    }

    @Override
    public PaymentDto refundPayment(UUID paymentId, RefundRequest request) {
        log.info("Refunding payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new InvalidPaymentStateException(
                    "Only completed payments can be refunded. Current status: " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAt(Instant.now());
        payment.setFailureReason("Refund: " + request.getReason());

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment {} refunded successfully", paymentId);

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    public PaymentDto cancelPayment(UUID paymentId) {
        log.info("Cancelling payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException(
                    "Only pending payments can be cancelled. Current status: " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.CANCELLED);

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment {} cancelled successfully", paymentId);

        return paymentMapper.toDto(savedPayment);
    }

    private boolean simulatePaymentProcessing(ProcessPaymentRequest request) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return true;
    }

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
