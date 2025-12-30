package com.example.orderservice.service;

import com.example.orderservice.dto.order.*;
import com.example.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderDto createOrder(UUID customerId, CreateOrderRequest request);

    OrderDto getOrderById(UUID orderId);

    OrderDto getOrderByIdForCustomer(UUID orderId, UUID customerId);

    Page<OrderSummaryDto> getOrdersByCustomer(UUID customerId, Pageable pageable);

    Page<OrderSummaryDto> getOrdersByRestaurant(UUID restaurantId, Pageable pageable);

    Page<OrderSummaryDto> getAllOrders(Pageable pageable);

    Page<OrderSummaryDto> getOrdersByStatus(OrderStatus status, Pageable pageable);

    List<OrderSummaryDto> getActiveOrdersByRestaurant(UUID restaurantId);

    OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request);

    OrderDto cancelOrder(UUID orderId, UUID customerId, CancelOrderRequest request);

    OrderDto cancelOrderByAdmin(UUID orderId, CancelOrderRequest request);

    OrderDto updateOrder(UUID orderId, UUID customerId, UpdateOrderRequest request);

    OrderDto updateOrderByAdmin(UUID orderId, UpdateOrderRequest request);

    void deleteOrder(UUID orderId);

    void handleOrderAccepted(UUID orderId, Integer estimatedPrepTimeMinutes);

    void handleOrderRejected(UUID orderId, String reason);

    void handleOrderReady(UUID orderId);

    void handleDeliveryStatusChanged(UUID orderId, String newStatus);
}
