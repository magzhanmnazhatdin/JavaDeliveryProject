package com.example.deliveryservice.kafka;

import com.example.deliveryservice.dto.event.OrderAcceptedEvent;
import com.example.deliveryservice.dto.event.OrderReadyEvent;
import com.example.deliveryservice.service.DeliveryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsListener {

    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.order-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderEvent(String message) {
        try {
            log.info("Received order event: {}", message);

            JsonNode jsonNode = objectMapper.readTree(message);
            String eventType = jsonNode.has("eventType") ? jsonNode.get("eventType").asText() : "";

            switch (eventType) {
                case "OrderAccepted" -> handleOrderAccepted(message);
                case "OrderReady" -> handleOrderReady(message);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", message, e);
        }
    }

    private void handleOrderAccepted(String message) {
        try {
            OrderAcceptedEvent event = objectMapper.readValue(message, OrderAcceptedEvent.class);
            log.info("Processing OrderAcceptedEvent for order: {}", event.getOrderId());
            deliveryService.createDeliveryFromOrderAccepted(event);
            log.info("Delivery created successfully for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error handling OrderAcceptedEvent", e);
        }
    }

    private void handleOrderReady(String message) {
        try {
            OrderReadyEvent event = objectMapper.readValue(message, OrderReadyEvent.class);
            log.info("Processing OrderReadyEvent for order: {}", event.getOrderId());
            deliveryService.handleOrderReady(event.getOrderId());
            log.info("Order ready handled successfully for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error handling OrderReadyEvent", e);
        }
    }
}
