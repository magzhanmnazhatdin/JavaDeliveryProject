package com.example.orderservice.kafka;

import com.example.orderservice.dto.event.DeliveryStatusChangedEvent;
import com.example.orderservice.dto.event.OrderAcceptedEvent;
import com.example.orderservice.dto.event.OrderReadyEvent;
import com.example.orderservice.dto.event.OrderRejectedEvent;
import com.example.orderservice.service.OrderService;
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

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.topics.restaurant-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleRestaurantEvents(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String eventType = jsonNode.has("eventType") ? jsonNode.get("eventType").asText() : "";

            log.info("Received restaurant event: {}", eventType);

            switch (eventType) {
                case "ORDER_ACCEPTED":
                    OrderAcceptedEvent acceptedEvent = objectMapper.treeToValue(jsonNode, OrderAcceptedEvent.class);
                    handleOrderAccepted(acceptedEvent);
                    break;
                case "ORDER_REJECTED":
                    OrderRejectedEvent rejectedEvent = objectMapper.treeToValue(jsonNode, OrderRejectedEvent.class);
                    handleOrderRejected(rejectedEvent);
                    break;
                case "ORDER_READY":
                    OrderReadyEvent readyEvent = objectMapper.treeToValue(jsonNode, OrderReadyEvent.class);
                    handleOrderReady(readyEvent);
                    break;
                default:
                    log.debug("Ignoring unknown restaurant event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing restaurant event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.topics.delivery-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleDeliveryEvents(String message) {
        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String eventType = jsonNode.has("eventType") ? jsonNode.get("eventType").asText() : "";

            log.info("Received delivery event: {}", eventType);

            if ("DELIVERY_STATUS_CHANGED".equals(eventType)) {
                DeliveryStatusChangedEvent event = objectMapper.treeToValue(jsonNode, DeliveryStatusChangedEvent.class);
                handleDeliveryStatusChanged(event);
            } else {
                log.debug("Ignoring delivery event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing delivery event: {}", e.getMessage(), e);
        }
    }

    private void handleOrderAccepted(OrderAcceptedEvent event) {
        log.info("Processing ORDER_ACCEPTED for order: {}", event.getOrderId());
        orderService.handleOrderAccepted(event.getOrderId(), event.getEstimatedPrepTimeMinutes());
    }

    private void handleOrderRejected(OrderRejectedEvent event) {
        log.info("Processing ORDER_REJECTED for order: {}", event.getOrderId());
        orderService.handleOrderRejected(event.getOrderId(), event.getRejectionReason());
    }

    private void handleOrderReady(OrderReadyEvent event) {
        log.info("Processing ORDER_READY for order: {}", event.getOrderId());
        orderService.handleOrderReady(event.getOrderId());
    }

    private void handleDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        log.info("Processing DELIVERY_STATUS_CHANGED for order: {}, new status: {}",
                event.getOrderId(), event.getNewStatus());
        orderService.handleDeliveryStatusChanged(event.getOrderId(), event.getNewStatus());
    }
}
