package com.example.orderservice.controller;

import com.example.orderservice.dto.order.*;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.PaymentMethod;
import com.example.orderservice.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("Should create order when user is customer")
    void createOrder_AsCustomer_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .restaurantId(restaurantId)
                .deliveryAddress("123 Main St")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(
                        CreateOrderRequest.OrderItemRequest.builder()
                                .menuItemId(UUID.randomUUID())
                                .name("Pizza")
                                .quantity(2)
                                .price(BigDecimal.valueOf(15.99))
                                .build()
                ))
                .build();

        OrderDto response = OrderDto.builder()
                .id(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(31.98))
                .deliveryAddress("123 Main St")
                .createdAt(Instant.now())
                .build();

        when(orderService.createOrder(any(UUID.class), any(CreateOrderRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(customerId.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.deliveryAddress").value("123 Main St"));
    }

    @Test
    @DisplayName("Should return 401 when creating order without authentication")
    void createOrder_Unauthorized() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .restaurantId(UUID.randomUUID())
                .deliveryAddress("123 Main St")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get order by ID when user is admin")
    void getOrder_AsAdmin_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        OrderDto orderDto = OrderDto.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(50.00))
                .build();

        when(orderService.getOrderById(orderId)).thenReturn(orderDto);

        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should get my orders")
    void getMyOrders_Success() throws Exception {
        UUID customerId = UUID.randomUUID();

        OrderSummaryDto summaryDto = OrderSummaryDto.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(25.00))
                .build();

        Page<OrderSummaryDto> page = new PageImpl<>(List.of(summaryDto));

        when(orderService.getOrdersByCustomer(eq(customerId), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders/my-orders")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(customerId.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Should update order status when user is restaurant")
    void updateOrderStatus_AsRestaurant_Success() throws Exception {
        UUID orderId = UUID.randomUUID();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        OrderDto updatedOrder = OrderDto.builder()
                .id(orderId)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderService.updateOrderStatus(eq(orderId), any(UpdateOrderStatusRequest.class)))
                .thenReturn(updatedOrder);

        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("RESTAURANT")))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Should return 403 when customer tries to update order status")
    void updateOrderStatus_AsCustomer_Forbidden() throws Exception {
        UUID orderId = UUID.randomUUID();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should cancel order when user is customer and owns the order")
    void cancelOrder_AsCustomer_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        CancelOrderRequest request = new CancelOrderRequest();
        request.setReason("Changed my mind");

        OrderDto cancelledOrder = OrderDto.builder()
                .id(orderId)
                .customerId(customerId)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelOrder(eq(orderId), eq(customerId), any(CancelOrderRequest.class)))
                .thenReturn(cancelledOrder);

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(customerId.toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Should get orders by status when user is admin")
    void getOrdersByStatus_AsAdmin_Success() throws Exception {
        OrderSummaryDto summaryDto = OrderSummaryDto.builder()
                .id(UUID.randomUUID())
                .status(OrderStatus.PENDING)
                .build();

        Page<OrderSummaryDto> page = new PageImpl<>(List.of(summaryDto));

        when(orderService.getOrdersByStatus(eq(OrderStatus.PENDING), any())).thenReturn(page);

        mockMvc.perform(get("/api/orders/status/{status}", OrderStatus.PENDING)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should return 400 for invalid request")
    void createOrder_InvalidRequest_BadRequest() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .restaurantId(null) // Invalid: null restaurant
                .deliveryAddress("")
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
