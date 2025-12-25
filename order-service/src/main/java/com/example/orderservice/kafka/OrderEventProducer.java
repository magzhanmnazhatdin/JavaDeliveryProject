package com.example.orderservice.kafka;

import com.example.orderservice.dto.event.OrderCancelledEvent;
import com.example.orderservice.dto.event.OrderCreatedEvent;
import com.example.orderservice.dto.event.PaymentCompletedEvent;
import com.example.orderservice.dto.event.PaymentFailedEvent;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${app.kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void sendOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Sending ORDER_CREATED event for order: {}", event.getOrderId());
        kafkaTemplate.send(orderEventsTopic, event.getOrderId().toString(), event);
    }

    public void sendOrderCancelledEvent(Order order, String reason) {
        OrderCancelledEvent event = OrderCancelledEvent.builder()
                .eventType("ORDER_CANCELLED")
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .restaurantId(order.getRestaurantId())
                .cancellationReason(reason)
                .cancelledAt(Instant.now())
                .build();

        log.info("Sending ORDER_CANCELLED event for order: {}", order.getId());
        kafkaTemplate.send(orderEventsTopic, order.getId().toString(), event);
    }

    public void sendPaymentCompletedEvent(Payment payment) {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventType("PAYMENT_COMPLETED")
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .customerId(payment.getOrder().getCustomerId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .transactionId(payment.getTransactionId())
                .paidAt(payment.getPaidAt())
                .build();

        log.info("Sending PAYMENT_COMPLETED event for order: {}", payment.getOrder().getId());
        kafkaTemplate.send(paymentEventsTopic, payment.getOrder().getId().toString(), event);
    }

    public void sendPaymentFailedEvent(Payment payment, String reason) {
        PaymentFailedEvent event = PaymentFailedEvent.builder()
                .eventType("PAYMENT_FAILED")
                .paymentId(payment.getId())
                .orderId(payment.getOrder().getId())
                .customerId(payment.getOrder().getCustomerId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod().name())
                .failureReason(reason)
                .failedAt(Instant.now())
                .build();

        log.info("Sending PAYMENT_FAILED event for order: {}", payment.getOrder().getId());
        kafkaTemplate.send(paymentEventsTopic, payment.getOrder().getId().toString(), event);
    }
}
