package com.example.orderservice.service;

import com.example.orderservice.dto.payment.*;
import com.example.orderservice.entity.*;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.PaymentNotFoundException;
import com.example.orderservice.exception.InvalidPaymentStateException;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.mapper.PaymentMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private PaymentDto paymentDto;
    private Order order;
    private UUID orderId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(50.00))
                .build();

        payment = Payment.builder()
                .id(paymentId)
                .order(order)
                .amount(BigDecimal.valueOf(50.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        paymentDto = PaymentDto.builder()
                .id(paymentId)
                .orderId(orderId)
                .amount(BigDecimal.valueOf(50.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .status(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("Should get payment by order ID successfully")
    void getPaymentByOrderId_Success() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(paymentDto);

        PaymentDto result = paymentService.getPaymentByOrderId(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when payment not found")
    void getPaymentByOrderId_NotFound() {
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(orderId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found");
    }

    @Test
    @DisplayName("Should process payment successfully")
    void processPayment_Success() {
        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .amount(BigDecimal.valueOf(50.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        order.setPayment(payment);

        Payment processedPayment = Payment.builder()
                .id(paymentId)
                .order(order)
                .amount(BigDecimal.valueOf(50.00))
                .status(PaymentStatus.COMPLETED)
                .transactionId("TXN-12345678")
                .paidAt(Instant.now())
                .build();

        PaymentDto processedDto = PaymentDto.builder()
                .id(paymentId)
                .orderId(orderId)
                .status(PaymentStatus.COMPLETED)
                .transactionId("TXN-12345678")
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenReturn(processedPayment);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(processedDto);

        PaymentDto result = paymentService.processPayment(request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should throw InvalidPaymentStateException when processing already completed payment")
    void processPayment_AlreadyCompleted_ThrowsException() {
        payment.setStatus(PaymentStatus.COMPLETED);
        order.setPayment(payment);

        ProcessPaymentRequest request = ProcessPaymentRequest.builder()
                .orderId(orderId)
                .amount(BigDecimal.valueOf(50.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(request))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("cannot be processed");
    }

    @Test
    @DisplayName("Should refund payment successfully")
    void refundPayment_Success() {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId("TXN-12345678");

        RefundRequest request = new RefundRequest();
        request.setReason("Customer cancelled");

        Payment refundedPayment = Payment.builder()
                .id(paymentId)
                .order(order)
                .amount(BigDecimal.valueOf(50.00))
                .status(PaymentStatus.REFUNDED)
                .refundedAt(Instant.now())
                .build();

        PaymentDto refundedDto = PaymentDto.builder()
                .id(paymentId)
                .orderId(orderId)
                .status(PaymentStatus.REFUNDED)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(refundedPayment);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(refundedDto);

        PaymentDto result = paymentService.refundPayment(paymentId, request);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("Should throw InvalidPaymentStateException when refunding non-completed payment")
    void refundPayment_NotCompleted_ThrowsException() {
        payment.setStatus(PaymentStatus.PENDING);
        RefundRequest request = new RefundRequest();
        request.setReason("Test");

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment(paymentId, request))
                .isInstanceOf(InvalidPaymentStateException.class)
                .hasMessageContaining("Only completed payments can be refunded");
    }
}
