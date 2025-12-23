package com.example.restaurantservice.kafka;

import com.example.restaurantservice.dto.event.OrderAcceptedEvent;
import com.example.restaurantservice.dto.event.OrderReadyEvent;
import com.example.restaurantservice.dto.event.OrderRejectedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.order-events}")
    private String orderEventsTopic;

    public void sendOrderAcceptedEvent(OrderAcceptedEvent event) {
        log.info("Sending OrderAcceptedEvent for order: {}", event.getOrderId());
        kafkaTemplate.send(orderEventsTopic, event.getOrderId().toString(), event);
        log.debug("OrderAcceptedEvent sent successfully");
    }

    public void sendOrderRejectedEvent(OrderRejectedEvent event) {
        log.info("Sending OrderRejectedEvent for order: {}", event.getOrderId());
        kafkaTemplate.send(orderEventsTopic, event.getOrderId().toString(), event);
        log.debug("OrderRejectedEvent sent successfully");
    }

    public void sendOrderReadyEvent(OrderReadyEvent event) {
        log.info("Sending OrderReadyEvent for order: {}", event.getOrderId());
        kafkaTemplate.send(orderEventsTopic, event.getOrderId().toString(), event);
        log.debug("OrderReadyEvent sent successfully");
    }
}
