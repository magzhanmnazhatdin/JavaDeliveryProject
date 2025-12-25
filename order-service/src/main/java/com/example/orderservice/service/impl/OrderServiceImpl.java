package com.example.orderservice.service.impl;

import com.example.orderservice.dto.order.*;
import com.example.orderservice.entity.*;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.InvalidOrderStateException;
import com.example.orderservice.exception.UnauthorizedAccessException;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.mapper.OrderItemMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderEventProducer orderEventProducer;

    private static final List<OrderStatus> ACTIVE_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.ACCEPTED_BY_RESTAURANT,
            OrderStatus.PREPARING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.PICKED_UP,
            OrderStatus.IN_DELIVERY
    );

    private static final List<OrderStatus> CANCELLABLE_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.ACCEPTED_BY_RESTAURANT
    );

    @Override
    public OrderDto createOrder(UUID customerId, CreateOrderRequest request) {
        log.info("Creating order for customer: {} at restaurant: {}", customerId, request.getRestaurantId());

        BigDecimal totalPrice = calculateTotalPrice(request.getItems());

        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(request.getRestaurantId())
                .status(OrderStatus.PENDING)
                .totalPrice(totalPrice)
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryLat(request.getDeliveryLat())
                .deliveryLng(request.getDeliveryLng())
                .customerNotes(request.getCustomerNotes())
                .build();

        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = orderItemMapper.toEntity(itemRequest);
            order.addItem(item);
        }

        Payment payment = Payment.builder()
                .order(order)
                .amount(totalPrice)
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING)
                .build();
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());

        orderEventProducer.sendOrderCreatedEvent(orderMapper.toOrderCreatedEvent(savedOrder));

        return orderMapper.toDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderById(UUID orderId) {
        Order order = findOrderById(orderId);
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrderByIdForCustomer(UUID orderId, UUID customerId) {
        Order order = findOrderById(orderId);
        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You don't have access to this order");
        }
        return orderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getOrdersByCustomer(UUID customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable)
                .map(orderMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getOrdersByRestaurant(UUID restaurantId, Pageable pageable) {
        return orderRepository.findByRestaurantId(restaurantId, pageable)
                .map(orderMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable)
                .map(orderMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryDto> getActiveOrdersByRestaurant(UUID restaurantId) {
        List<Order> orders = orderRepository.findActiveOrdersByRestaurant(restaurantId, ACTIVE_STATUSES);
        return orderMapper.toSummaryDtoList(orders);
    }

    @Override
    public OrderDto updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        Order order = findOrderById(orderId);

        validateStatusTransition(order.getStatus(), request.getStatus());

        order.setStatus(request.getStatus());

        if (request.getStatus() == OrderStatus.DELIVERED) {
            order.setDeliveredAt(Instant.now());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {}", orderId, request.getStatus());

        return orderMapper.toDto(savedOrder);
    }

    @Override
    public OrderDto cancelOrder(UUID orderId, UUID customerId, CancelOrderRequest request) {
        Order order = findOrderById(orderId);

        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You don't have access to this order");
        }

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Order cannot be cancelled in status: " + order.getStatus()
            );
        }

        return performCancellation(order, request.getReason());
    }

    @Override
    public OrderDto cancelOrderByAdmin(UUID orderId, CancelOrderRequest request) {
        Order order = findOrderById(orderId);
        return performCancellation(order, request.getReason());
    }

    @Override
    public OrderDto updateOrder(UUID orderId, UUID customerId, UpdateOrderRequest request) {
        Order order = findOrderById(orderId);

        if (!order.getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You don't have access to this order");
        }

        if (!CANCELLABLE_STATUSES.contains(order.getStatus())) {
            throw new InvalidOrderStateException(
                    "Order cannot be updated in status: " + order.getStatus()
            );
        }

        return performUpdate(order, request);
    }

    @Override
    public OrderDto updateOrderByAdmin(UUID orderId, UpdateOrderRequest request) {
        Order order = findOrderById(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED ||
            order.getStatus() == OrderStatus.CANCELLED ||
            order.getStatus() == OrderStatus.REJECTED) {
            throw new InvalidOrderStateException(
                    "Order cannot be updated in status: " + order.getStatus()
            );
        }

        return performUpdate(order, request);
    }

    @Override
    public void deleteOrder(UUID orderId) {
        Order order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING &&
            order.getStatus() != OrderStatus.CANCELLED &&
            order.getStatus() != OrderStatus.REJECTED) {
            throw new InvalidOrderStateException(
                    "Only pending, cancelled, or rejected orders can be deleted. Current status: " + order.getStatus()
            );
        }

        orderRepository.delete(order);
        log.info("Order {} deleted", orderId);
    }

    private OrderDto performUpdate(Order order, UpdateOrderRequest request) {
        if (request.getDeliveryAddress() != null) {
            order.setDeliveryAddress(request.getDeliveryAddress());
        }
        if (request.getDeliveryLat() != null) {
            order.setDeliveryLat(request.getDeliveryLat());
        }
        if (request.getDeliveryLng() != null) {
            order.setDeliveryLng(request.getDeliveryLng());
        }
        if (request.getCustomerNotes() != null) {
            order.setCustomerNotes(request.getCustomerNotes());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} updated", order.getId());

        return orderMapper.toDto(savedOrder);
    }

    @Override
    public void handleOrderAccepted(UUID orderId, Integer estimatedPrepTimeMinutes) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.ACCEPTED_BY_RESTAURANT);
        order.setConfirmedAt(Instant.now());

        if (estimatedPrepTimeMinutes != null) {
            order.setEstimatedDeliveryTime(
                    Instant.now().plus(estimatedPrepTimeMinutes + 30, ChronoUnit.MINUTES)
            );
        }

        orderRepository.save(order);
        log.info("Order {} accepted by restaurant", orderId);
    }

    @Override
    public void handleOrderRejected(UUID orderId, String reason) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectionReason(reason);
        order.setCancelledAt(Instant.now());

        Payment payment = order.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(Instant.now());
        } else if (payment != null) {
            payment.setStatus(PaymentStatus.CANCELLED);
        }

        orderRepository.save(order);
        log.info("Order {} rejected by restaurant: {}", orderId, reason);
    }

    @Override
    public void handleOrderReady(UUID orderId) {
        Order order = findOrderById(orderId);
        order.setStatus(OrderStatus.READY_FOR_PICKUP);
        orderRepository.save(order);
        log.info("Order {} is ready for pickup", orderId);
    }

    @Override
    public void handleDeliveryStatusChanged(UUID orderId, String newStatus) {
        Order order = findOrderById(orderId);

        switch (newStatus) {
            case "PICKED_UP":
                order.setStatus(OrderStatus.PICKED_UP);
                break;
            case "IN_TRANSIT":
                order.setStatus(OrderStatus.IN_DELIVERY);
                break;
            case "DELIVERED":
                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredAt(Instant.now());
                break;
            case "CANCELLED":
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelledAt(Instant.now());
                break;
            default:
                log.warn("Unknown delivery status: {}", newStatus);
                return;
        }

        orderRepository.save(order);
        log.info("Order {} status updated based on delivery: {}", orderId, newStatus);
    }

    private Order findOrderById(UUID orderId) {
        return orderRepository.findByIdWithItemsAndPayment(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
    }

    private BigDecimal calculateTotalPrice(List<CreateOrderRequest.OrderItemRequest> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        if (current == OrderStatus.DELIVERED || current == OrderStatus.CANCELLED || current == OrderStatus.REJECTED) {
            throw new InvalidOrderStateException(
                    "Cannot change status of a finalized order: " + current
            );
        }
    }

    private OrderDto performCancellation(Order order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setRejectionReason(reason);
        order.setCancelledAt(Instant.now());

        Payment payment = order.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.COMPLETED) {
            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAt(Instant.now());
        } else if (payment != null) {
            payment.setStatus(PaymentStatus.CANCELLED);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled: {}", order.getId(), reason);

        orderEventProducer.sendOrderCancelledEvent(savedOrder, reason);

        return orderMapper.toDto(savedOrder);
    }
}
