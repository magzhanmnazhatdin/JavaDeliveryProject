package com.example.restaurantservice.kafka;

import com.example.restaurantservice.dto.event.OrderCreatedEvent;
import com.example.restaurantservice.service.RestaurantOrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventsListener {

    private final RestaurantOrderService orderService;
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

            if ("OrderCreated".equals(eventType)) {
                handleOrderCreated(message);
            } else {
                log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", message, e);
        }
    }

    private void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            log.info("Processing OrderCreatedEvent for order: {}", event.getOrderId());

            UUID restaurantId = event.getRestaurantId();
            orderService.createOrderFromEvent(event, restaurantId);

            log.info("Restaurant order created successfully for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error handling OrderCreatedEvent", e);
        }
    }
}
