package com.example.deliveryservice.kafka;

import com.example.deliveryservice.dto.delivery.DeliveryDto;
import com.example.deliveryservice.dto.event.OrderAcceptedEvent;
import com.example.deliveryservice.entity.DeliveryStatus;
import com.example.deliveryservice.service.DeliveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventsListenerTest {

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private OrderEventsListener orderEventsListener;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        orderEventsListener = new OrderEventsListener(deliveryService, objectMapper);
    }

    @Test
    @DisplayName("Should process OrderAccepted event and create delivery")
    void handleOrderEvent_OrderAccepted_CreatesDelivery() throws Exception {
        OrderAcceptedEvent event = OrderAcceptedEvent.builder()
                .eventType("OrderAccepted")
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .deliveryAddress("123 Main St")
                .totalPrice(BigDecimal.valueOf(25.99))
                .acceptedAt(Instant.now())
                .build();

        String message = objectMapper.writeValueAsString(event);

        DeliveryDto deliveryDto = DeliveryDto.builder()
                .id(UUID.randomUUID())
                .orderId(event.getOrderId())
                .status(DeliveryStatus.PENDING)
                .build();

        when(deliveryService.createDeliveryFromOrderAccepted(any(OrderAcceptedEvent.class)))
                .thenReturn(deliveryDto);

        orderEventsListener.handleOrderEvent(message);

        verify(deliveryService).createDeliveryFromOrderAccepted(any(OrderAcceptedEvent.class));
    }

    @Test
    @DisplayName("Should process OrderReady event")
    void handleOrderEvent_OrderReady_HandlesOrderReady() throws Exception {
        UUID orderId = UUID.randomUUID();
        String message = String.format(
                "{\"eventType\":\"OrderReady\",\"orderId\":\"%s\",\"restaurantId\":\"%s\",\"readyAt\":\"%s\"}",
                orderId, UUID.randomUUID(), Instant.now().toString()
        );

        orderEventsListener.handleOrderEvent(message);

        verify(deliveryService).handleOrderReady(orderId);
    }

    @Test
    @DisplayName("Should ignore unknown event types")
    void handleOrderEvent_UnknownEventType_Ignores() throws Exception {
        String message = "{\"eventType\":\"UnknownEvent\",\"data\":\"test\"}";

        orderEventsListener.handleOrderEvent(message);

        verifyNoInteractions(deliveryService);
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void handleOrderEvent_MalformedJson_DoesNotThrow() {
        String malformedMessage = "not a valid json";

        // Should not throw exception
        orderEventsListener.handleOrderEvent(malformedMessage);

        verifyNoInteractions(deliveryService);
    }
}
