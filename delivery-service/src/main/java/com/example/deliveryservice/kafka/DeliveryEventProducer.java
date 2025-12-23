package com.example.deliveryservice.kafka;

import com.example.deliveryservice.dto.event.CourierAssignedEvent;
import com.example.deliveryservice.dto.event.DeliveryStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeliveryEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.delivery-events}")
    private String deliveryEventsTopic;

    public void sendCourierAssignedEvent(CourierAssignedEvent event) {
        log.info("Sending CourierAssignedEvent for delivery: {}", event.getDeliveryId());
        kafkaTemplate.send(deliveryEventsTopic, event.getOrderId().toString(), event);
        log.debug("CourierAssignedEvent sent successfully");
    }

    public void sendDeliveryStatusChangedEvent(DeliveryStatusChangedEvent event) {
        log.info("Sending DeliveryStatusChangedEvent for delivery: {}, new status: {}",
                event.getDeliveryId(), event.getNewStatus());
        kafkaTemplate.send(deliveryEventsTopic, event.getOrderId().toString(), event);
        log.debug("DeliveryStatusChangedEvent sent successfully");
    }
}
