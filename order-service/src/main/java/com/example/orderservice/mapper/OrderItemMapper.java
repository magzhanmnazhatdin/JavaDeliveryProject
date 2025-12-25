package com.example.orderservice.mapper;

import com.example.orderservice.dto.order.CreateOrderRequest;
import com.example.orderservice.dto.order.OrderItemDto;
import com.example.orderservice.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderItemMapper {

    public OrderItemDto toDto(OrderItem item) {
        if (item == null) {
            return null;
        }

        return OrderItemDto.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItemId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .specialInstructions(item.getSpecialInstructions())
                .build();
    }

    public OrderItem toEntity(CreateOrderRequest.OrderItemRequest request) {
        if (request == null) {
            return null;
        }

        return OrderItem.builder()
                .menuItemId(request.getMenuItemId())
                .name(request.getName())
                .price(request.getPrice())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .specialInstructions(request.getSpecialInstructions())
                .build();
    }

    public List<OrderItemDto> toDtoList(List<OrderItem> items) {
        return items.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<OrderItem> toEntityList(List<CreateOrderRequest.OrderItemRequest> requests) {
        return requests.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }
}
