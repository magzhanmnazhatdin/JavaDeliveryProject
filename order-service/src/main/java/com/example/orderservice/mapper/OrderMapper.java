package com.example.orderservice.mapper;

import com.example.orderservice.dto.order.*;
import com.example.orderservice.dto.event.OrderCreatedEvent;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;

    public OrderMapper(OrderItemMapper orderItemMapper, PaymentMapper paymentMapper) {
        this.orderItemMapper = orderItemMapper;
        this.paymentMapper = paymentMapper;
    }

    public OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }

        return OrderDto.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .restaurantId(order.getRestaurantId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .deliveryAddress(order.getDeliveryAddress())
                .deliveryLat(order.getDeliveryLat())
                .deliveryLng(order.getDeliveryLng())
                .customerNotes(order.getCustomerNotes())
                .rejectionReason(order.getRejectionReason())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .confirmedAt(order.getConfirmedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .items(order.getItems().stream()
                        .map(orderItemMapper::toDto)
                        .collect(Collectors.toList()))
                .payment(order.getPayment() != null ? paymentMapper.toDto(order.getPayment()) : null)
                .build();
    }

    public OrderSummaryDto toSummaryDto(Order order) {
        if (order == null) {
            return null;
        }

        return OrderSummaryDto.builder()
                .id(order.getId())
                .restaurantId(order.getRestaurantId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .deliveryAddress(order.getDeliveryAddress())
                .itemCount(order.getItems().size())
                .createdAt(order.getCreatedAt())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .build();
    }

    public List<OrderDto> toDtoList(List<Order> orders) {
        return orders.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<OrderSummaryDto> toSummaryDtoList(List<Order> orders) {
        return orders.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    public OrderCreatedEvent toOrderCreatedEvent(Order order) {
        List<OrderCreatedEvent.OrderItemEvent> itemEvents = order.getItems().stream()
                .map(this::toOrderItemEvent)
                .collect(Collectors.toList());

        return OrderCreatedEvent.builder()
                .eventType("ORDER_CREATED")
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .restaurantId(order.getRestaurantId())
                .totalPrice(order.getTotalPrice())
                .deliveryAddress(order.getDeliveryAddress())
                .customerNotes(order.getCustomerNotes())
                .items(itemEvents)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private OrderCreatedEvent.OrderItemEvent toOrderItemEvent(OrderItem item) {
        return OrderCreatedEvent.OrderItemEvent.builder()
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .specialInstructions(item.getSpecialInstructions())
                .build();
    }
}
