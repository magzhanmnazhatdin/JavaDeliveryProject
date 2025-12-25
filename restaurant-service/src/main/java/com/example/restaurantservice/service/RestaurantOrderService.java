package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.event.OrderCreatedEvent;
import com.example.restaurantservice.dto.order.AcceptOrderRequest;
import com.example.restaurantservice.dto.order.RejectOrderRequest;
import com.example.restaurantservice.dto.order.RestaurantOrderDto;
import com.example.restaurantservice.entity.RestaurantOrderStatus;

import java.util.List;
import java.util.UUID;

public interface RestaurantOrderService {

    RestaurantOrderDto createOrderFromEvent(OrderCreatedEvent event, UUID restaurantId);

    RestaurantOrderDto getOrderById(UUID id);

    RestaurantOrderDto getOrderByOrderId(UUID orderId);

    List<RestaurantOrderDto> getOrdersByRestaurant(UUID restaurantId);

    List<RestaurantOrderDto> getOrdersByRestaurantAndStatus(UUID restaurantId, RestaurantOrderStatus status);

    List<RestaurantOrderDto> getPendingOrders(UUID restaurantId);

    List<RestaurantOrderDto> getActiveOrders(UUID restaurantId);

    RestaurantOrderDto acceptOrder(UUID id, AcceptOrderRequest request);

    RestaurantOrderDto rejectOrder(UUID id, RejectOrderRequest request);

    RestaurantOrderDto startPreparing(UUID id);

    RestaurantOrderDto markAsReady(UUID id);

    RestaurantOrderDto markAsPickedUp(UUID id);

    void deleteOrder(UUID id);
}
