package com.example.restaurantservice.service.impl;

import com.example.restaurantservice.dto.event.OrderAcceptedEvent;
import com.example.restaurantservice.dto.event.OrderCreatedEvent;
import com.example.restaurantservice.dto.event.OrderReadyEvent;
import com.example.restaurantservice.dto.event.OrderRejectedEvent;
import com.example.restaurantservice.dto.order.AcceptOrderRequest;
import com.example.restaurantservice.dto.order.RejectOrderRequest;
import com.example.restaurantservice.dto.order.RestaurantOrderDto;
import com.example.restaurantservice.entity.Restaurant;
import com.example.restaurantservice.entity.RestaurantOrder;
import com.example.restaurantservice.entity.RestaurantOrderStatus;
import com.example.restaurantservice.exception.BadRequestException;
import com.example.restaurantservice.exception.ConflictException;
import com.example.restaurantservice.exception.ResourceNotFoundException;
import com.example.restaurantservice.kafka.RestaurantEventProducer;
import com.example.restaurantservice.mapper.RestaurantOrderMapper;
import com.example.restaurantservice.repository.RestaurantOrderRepository;
import com.example.restaurantservice.repository.RestaurantRepository;
import com.example.restaurantservice.service.RestaurantOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RestaurantOrderServiceImpl implements RestaurantOrderService {

    private final RestaurantOrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantOrderMapper orderMapper;
    private final RestaurantEventProducer eventProducer;

    @Override
    public RestaurantOrderDto createOrderFromEvent(OrderCreatedEvent event, UUID restaurantId) {
        log.info("Creating restaurant order from event for order: {}", event.getOrderId());

        if (orderRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Order {} already exists, skipping", event.getOrderId());
            return orderMapper.toDto(orderRepository.findByOrderId(event.getOrderId()).orElseThrow());
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        RestaurantOrder order = orderMapper.fromOrderCreatedEvent(event);
        order.setRestaurant(restaurant);
        RestaurantOrder savedOrder = orderRepository.save(order);

        log.info("Restaurant order created with ID: {}", savedOrder.getId());
        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantOrderDto getOrderById(UUID id) {
        log.debug("Getting order by ID: {}", id);
        RestaurantOrder order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantOrder", "id", id));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantOrderDto getOrderByOrderId(UUID orderId) {
        log.debug("Getting order by orderId: {}", orderId);
        RestaurantOrder order = orderRepository.findByOrderIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantOrder", "orderId", orderId));
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantOrderDto> getOrdersByRestaurant(UUID restaurantId) {
        log.debug("Getting orders for restaurant: {}", restaurantId);
        return orderRepository.findByRestaurantId(restaurantId).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantOrderDto> getOrdersByRestaurantAndStatus(UUID restaurantId, RestaurantOrderStatus status) {
        log.debug("Getting orders for restaurant {} with status {}", restaurantId, status);
        return orderRepository.findByRestaurantIdAndStatus(restaurantId, status).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantOrderDto> getPendingOrders(UUID restaurantId) {
        log.debug("Getting pending orders for restaurant: {}", restaurantId);
        return orderRepository.findByRestaurantIdAndStatus(restaurantId, RestaurantOrderStatus.PENDING).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantOrderDto> getActiveOrders(UUID restaurantId) {
        log.debug("Getting active orders for restaurant: {}", restaurantId);
        List<RestaurantOrderStatus> activeStatuses = List.of(
                RestaurantOrderStatus.PENDING,
                RestaurantOrderStatus.ACCEPTED,
                RestaurantOrderStatus.PREPARING
        );
        return orderRepository.findByRestaurantIdAndStatusIn(restaurantId, activeStatuses).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Override
    public RestaurantOrderDto acceptOrder(UUID id, AcceptOrderRequest request) {
        log.info("Accepting order: {}", id);
        RestaurantOrder order = findOrderById(id);

        validateStatusTransition(order.getStatus(), RestaurantOrderStatus.ACCEPTED);

        order.setStatus(RestaurantOrderStatus.ACCEPTED);
        order.setAcceptedAt(Instant.now());
        if (request != null && request.getEstimatedPrepTimeMinutes() != null) {
            order.setEstimatedPrepTimeMinutes(request.getEstimatedPrepTimeMinutes());
        }

        RestaurantOrder savedOrder = orderRepository.save(order);

        // Publish OrderAcceptedEvent
        Restaurant restaurant = order.getRestaurant();
        OrderAcceptedEvent event = OrderAcceptedEvent.builder()
                .eventType("ORDER_ACCEPTED")
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .restaurantAddress(restaurant.getAddress())
                .restaurantLat(restaurant.getLatitude())
                .restaurantLng(restaurant.getLongitude())
                .deliveryAddress(order.getDeliveryAddress())
                .totalPrice(order.getTotalPrice())
                .estimatedPrepTimeMinutes(order.getEstimatedPrepTimeMinutes())
                .customerNotes(order.getCustomerNotes())
                .acceptedAt(order.getAcceptedAt())
                .build();

        eventProducer.sendOrderAcceptedEvent(event);

        log.info("Order {} accepted successfully", id);
        return orderMapper.toDto(savedOrder);
    }

    @Override
    public RestaurantOrderDto rejectOrder(UUID id, RejectOrderRequest request) {
        log.info("Rejecting order: {}", id);
        RestaurantOrder order = findOrderById(id);

        validateStatusTransition(order.getStatus(), RestaurantOrderStatus.REJECTED);

        order.setStatus(RestaurantOrderStatus.REJECTED);
        order.setRejectedAt(Instant.now());
        order.setRejectionReason(request.getReason());

        RestaurantOrder savedOrder = orderRepository.save(order);

        // Publish OrderRejectedEvent
        OrderRejectedEvent event = OrderRejectedEvent.builder()
                .eventType("ORDER_REJECTED")
                .orderId(order.getOrderId())
                .restaurantId(order.getRestaurant().getId())
                .rejectionReason(request.getReason())
                .rejectedAt(order.getRejectedAt())
                .build();

        eventProducer.sendOrderRejectedEvent(event);

        log.info("Order {} rejected: {}", id, request.getReason());
        return orderMapper.toDto(savedOrder);
    }

    @Override
    public RestaurantOrderDto startPreparing(UUID id) {
        log.info("Starting preparation for order: {}", id);
        RestaurantOrder order = findOrderById(id);

        validateStatusTransition(order.getStatus(), RestaurantOrderStatus.PREPARING);

        order.setStatus(RestaurantOrderStatus.PREPARING);
        order.setPreparingAt(Instant.now());

        RestaurantOrder savedOrder = orderRepository.save(order);

        log.info("Order {} is now being prepared", id);
        return orderMapper.toDto(savedOrder);
    }

    @Override
    public RestaurantOrderDto markAsReady(UUID id) {
        log.info("Marking order as ready: {}", id);
        RestaurantOrder order = findOrderById(id);

        validateStatusTransition(order.getStatus(), RestaurantOrderStatus.READY);

        order.setStatus(RestaurantOrderStatus.READY);
        order.setReadyAt(Instant.now());

        RestaurantOrder savedOrder = orderRepository.save(order);

        // Publish OrderReadyEvent
        Restaurant restaurant = order.getRestaurant();
        OrderReadyEvent event = OrderReadyEvent.builder()
                .eventType("ORDER_READY")
                .orderId(order.getOrderId())
                .restaurantId(restaurant.getId())
                .restaurantName(restaurant.getName())
                .restaurantAddress(restaurant.getAddress())
                .readyAt(order.getReadyAt())
                .build();

        eventProducer.sendOrderReadyEvent(event);

        log.info("Order {} is ready for pickup", id);
        return orderMapper.toDto(savedOrder);
    }

    @Override
    public RestaurantOrderDto markAsPickedUp(UUID id) {
        log.info("Marking order as picked up: {}", id);
        RestaurantOrder order = findOrderById(id);

        validateStatusTransition(order.getStatus(), RestaurantOrderStatus.PICKED_UP);

        order.setStatus(RestaurantOrderStatus.PICKED_UP);

        RestaurantOrder savedOrder = orderRepository.save(order);

        log.info("Order {} has been picked up", id);
        return orderMapper.toDto(savedOrder);
    }

    @Override
    public void deleteOrder(UUID id) {
        log.info("Deleting restaurant order: {}", id);
        RestaurantOrder order = findOrderById(id);

        if (order.getStatus() != RestaurantOrderStatus.PENDING &&
            order.getStatus() != RestaurantOrderStatus.REJECTED &&
            order.getStatus() != RestaurantOrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Only pending, rejected, or cancelled orders can be deleted. Current status: " + order.getStatus()
            );
        }

        orderRepository.delete(order);
        log.info("Restaurant order {} deleted", id);
    }

    private RestaurantOrder findOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RestaurantOrder", "id", id));
    }

    private void validateStatusTransition(RestaurantOrderStatus currentStatus, RestaurantOrderStatus newStatus) {
        boolean valid = switch (currentStatus) {
            case PENDING -> newStatus == RestaurantOrderStatus.ACCEPTED || newStatus == RestaurantOrderStatus.REJECTED;
            case ACCEPTED -> newStatus == RestaurantOrderStatus.PREPARING || newStatus == RestaurantOrderStatus.CANCELLED;
            case PREPARING -> newStatus == RestaurantOrderStatus.READY || newStatus == RestaurantOrderStatus.CANCELLED;
            case READY -> newStatus == RestaurantOrderStatus.PICKED_UP || newStatus == RestaurantOrderStatus.CANCELLED;
            case REJECTED, PICKED_UP, CANCELLED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }
    }
}
