package com.example.deliveryservice.mapper;

import com.example.deliveryservice.dto.delivery.CreateDeliveryRequest;
import com.example.deliveryservice.dto.delivery.DeliveryDto;
import com.example.deliveryservice.dto.event.OrderAcceptedEvent;
import com.example.deliveryservice.entity.Delivery;
import com.example.deliveryservice.entity.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryMapper {

    private final CourierMapper courierMapper;

    public DeliveryDto toDto(Delivery delivery) {
        if (delivery == null) {
            return null;
        }
        return DeliveryDto.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .customerId(delivery.getCustomerId())
                .restaurantId(delivery.getRestaurantId())
                .courier(courierMapper.toDto(delivery.getCourier()))
                .deliveryAddress(delivery.getDeliveryAddress())
                .deliveryLat(delivery.getDeliveryLat())
                .deliveryLng(delivery.getDeliveryLng())
                .pickupAddress(delivery.getPickupAddress())
                .pickupLat(delivery.getPickupLat())
                .pickupLng(delivery.getPickupLng())
                .status(delivery.getStatus())
                .assignedAt(delivery.getAssignedAt())
                .pickedUpAt(delivery.getPickedUpAt())
                .deliveredAt(delivery.getDeliveredAt())
                .cancelledAt(delivery.getCancelledAt())
                .cancellationReason(delivery.getCancellationReason())
                .customerNotes(delivery.getCustomerNotes())
                .courierNotes(delivery.getCourierNotes())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }

    public Delivery toEntity(CreateDeliveryRequest request) {
        if (request == null) {
            return null;
        }
        return Delivery.builder()
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .restaurantId(request.getRestaurantId())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryLat(request.getDeliveryLat())
                .deliveryLng(request.getDeliveryLng())
                .pickupAddress(request.getPickupAddress())
                .pickupLat(request.getPickupLat())
                .pickupLng(request.getPickupLng())
                .customerNotes(request.getCustomerNotes())
                .status(DeliveryStatus.PENDING)
                .build();
    }

    public Delivery fromOrderAcceptedEvent(OrderAcceptedEvent event) {
        if (event == null) {
            return null;
        }
        return Delivery.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .restaurantId(event.getRestaurantId())
                .deliveryAddress(event.getDeliveryAddress())
                .deliveryLat(event.getDeliveryLat())
                .deliveryLng(event.getDeliveryLng())
                .pickupAddress(event.getPickupAddress())
                .pickupLat(event.getPickupLat())
                .pickupLng(event.getPickupLng())
                .customerNotes(event.getCustomerNotes())
                .status(DeliveryStatus.PENDING)
                .build();
    }
}
