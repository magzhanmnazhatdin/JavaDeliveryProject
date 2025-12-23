package com.example.restaurantservice.mapper;

import com.example.restaurantservice.dto.event.OrderCreatedEvent;
import com.example.restaurantservice.dto.order.RestaurantOrderDto;
import com.example.restaurantservice.dto.order.RestaurantOrderItemDto;
import com.example.restaurantservice.entity.RestaurantOrder;
import com.example.restaurantservice.entity.RestaurantOrderItem;
import com.example.restaurantservice.entity.RestaurantOrderStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RestaurantOrderMapper {

    public RestaurantOrderDto toDto(RestaurantOrder order) {
        if (order == null) {
            return null;
        }
        return RestaurantOrderDto.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .restaurantId(order.getRestaurant() != null ? order.getRestaurant().getId() : null)
                .customerId(order.getCustomerId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .deliveryAddress(order.getDeliveryAddress())
                .customerNotes(order.getCustomerNotes())
                .receivedAt(order.getReceivedAt())
                .acceptedAt(order.getAcceptedAt())
                .rejectedAt(order.getRejectedAt())
                .preparingAt(order.getPreparingAt())
                .readyAt(order.getReadyAt())
                .rejectionReason(order.getRejectionReason())
                .estimatedPrepTimeMinutes(order.getEstimatedPrepTimeMinutes())
                .items(order.getItems() != null ? order.getItems().stream().map(this::toItemDto).toList() : null)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public RestaurantOrderItemDto toItemDto(RestaurantOrderItem item) {
        if (item == null) {
            return null;
        }
        return RestaurantOrderItemDto.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItemId())
                .nameSnapshot(item.getNameSnapshot())
                .priceSnapshot(item.getPriceSnapshot())
                .quantity(item.getQuantity())
                .specialInstructions(item.getSpecialInstructions())
                .build();
    }

    public RestaurantOrder fromOrderCreatedEvent(OrderCreatedEvent event) {
        if (event == null) {
            return null;
        }
        RestaurantOrder order = RestaurantOrder.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .totalPrice(event.getTotalPrice())
                .deliveryAddress(event.getDeliveryAddress())
                .customerNotes(event.getCustomerNotes())
                .status(RestaurantOrderStatus.PENDING)
                .build();

        if (event.getItems() != null) {
            for (OrderCreatedEvent.OrderItemEvent itemEvent : event.getItems()) {
                RestaurantOrderItem item = RestaurantOrderItem.builder()
                        .menuItemId(itemEvent.getMenuItemId())
                        .nameSnapshot(itemEvent.getName())
                        .priceSnapshot(itemEvent.getPrice())
                        .quantity(itemEvent.getQuantity())
                        .specialInstructions(itemEvent.getSpecialInstructions())
                        .build();
                order.addItem(item);
            }
        }

        return order;
    }
}
