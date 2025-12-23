package com.example.restaurantservice.kafka;

import com.example.restaurantservice.dto.event.OrderCreatedEvent;
import com.example.restaurantservice.dto.order.RestaurantOrderDto;
import com.example.restaurantservice.entity.RestaurantOrderStatus;
import com.example.restaurantservice.service.RestaurantOrderService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventsListenerTest {

    @Mock
    private RestaurantOrderService orderService;

    @InjectMocks
    private OrderEventsListener orderEventsListener;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        orderEventsListener = new OrderEventsListener(orderService, objectMapper);
    }

    @Test
    @DisplayName("Should process OrderCreated event and create restaurant order")
    void handleOrderEvent_OrderCreated_CreatesOrder() throws Exception {
        UUID restaurantId = UUID.randomUUID();
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventType("OrderCreated")
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .restaurantId(restaurantId)
                .totalPrice(BigDecimal.valueOf(25.99))
                .deliveryAddress("123 Main St")
                .items(List.of())
                .createdAt(Instant.now())
                .build();

        String message = objectMapper.writeValueAsString(event);

        RestaurantOrderDto orderDto = RestaurantOrderDto.builder()
                .id(UUID.randomUUID())
                .orderId(event.getOrderId())
                .status(RestaurantOrderStatus.PENDING)
                .build();

        when(orderService.createOrderFromEvent(any(OrderCreatedEvent.class), eq(restaurantId)))
                .thenReturn(orderDto);

        orderEventsListener.handleOrderEvent(message);

        verify(orderService).createOrderFromEvent(any(OrderCreatedEvent.class), eq(restaurantId));
    }

    @Test
    @DisplayName("Should ignore unknown event types")
    void handleOrderEvent_UnknownEventType_Ignores() throws Exception {
        String message = "{\"eventType\":\"UnknownEvent\",\"data\":\"test\"}";

        orderEventsListener.handleOrderEvent(message);

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Should handle malformed JSON gracefully")
    void handleOrderEvent_MalformedJson_DoesNotThrow() {
        String malformedMessage = "not a valid json";

        // Should not throw exception
        orderEventsListener.handleOrderEvent(malformedMessage);

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Should ignore non-OrderCreated events")
    void handleOrderEvent_OtherEvents_Ignores() throws Exception {
        String message = "{\"eventType\":\"OrderAccepted\",\"orderId\":\"" + UUID.randomUUID() + "\"}";

        orderEventsListener.handleOrderEvent(message);

        verifyNoInteractions(orderService);
    }
}
