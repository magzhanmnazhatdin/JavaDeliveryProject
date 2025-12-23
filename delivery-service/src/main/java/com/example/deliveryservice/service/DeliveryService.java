package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.delivery.*;
import com.example.deliveryservice.dto.event.OrderAcceptedEvent;
import com.example.deliveryservice.entity.DeliveryStatus;

import java.util.List;
import java.util.UUID;

public interface DeliveryService {

    DeliveryDto createDelivery(CreateDeliveryRequest request);

    DeliveryDto createDeliveryFromOrderAccepted(OrderAcceptedEvent event);

    DeliveryDto getDeliveryById(UUID id);

    DeliveryDto getDeliveryByOrderId(UUID orderId);

    List<DeliveryDto> getDeliveriesByCourierId(UUID courierId);

    List<DeliveryDto> getDeliveriesByCustomerId(UUID customerId);

    List<DeliveryDto> getDeliveriesByStatus(DeliveryStatus status);

    List<DeliveryDto> getAllDeliveries();

    DeliveryDto assignCourier(UUID deliveryId, AssignCourierRequest request);

    DeliveryDto assignCourierAutomatically(UUID deliveryId);

    DeliveryDto updateDeliveryStatus(UUID deliveryId, UpdateDeliveryStatusRequest request);

    void handleOrderReady(UUID orderId);
}
