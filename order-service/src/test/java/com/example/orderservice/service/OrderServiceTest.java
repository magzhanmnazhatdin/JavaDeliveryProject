package com.example.orderservice.service;

import com.example.orderservice.dto.order.*;
import com.example.orderservice.entity.*;
import com.example.orderservice.exception.InvalidOrderStateException;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.exception.UnauthorizedAccessException;
import com.example.orderservice.kafka.OrderEventProducer;
import com.example.orderservice.mapper.OrderItemMapper;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderDto orderDto;
    private UUID customerId;
    private UUID restaurantId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        order = Order.builder()
                .id(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(25.99))
                .deliveryAddress("123 Main St")
                .createdAt(Instant.now())
                .build();

        orderDto = OrderDto.builder()
                .id(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(25.99))
                .deliveryAddress("123 Main St")
                .build();
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void getOrderById_Success() {
        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        OrderDto result = orderService.getOrderById(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).findByIdWithItemsAndPayment(orderId);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void getOrderById_NotFound() {
        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    @DisplayName("Should get order for customer when customer owns the order")
    void getOrderByIdForCustomer_Success() {
        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toDto(order)).thenReturn(orderDto);

        OrderDto result = orderService.getOrderByIdForCustomer(orderId, customerId);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when customer doesn't own the order")
    void getOrderByIdForCustomer_Unauthorized() {
        UUID anotherCustomerId = UUID.randomUUID();
        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderByIdForCustomer(orderId, anotherCustomerId))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    @DisplayName("Should get orders by customer with pagination")
    void getOrdersByCustomer_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(order));
        OrderSummaryDto summaryDto = OrderSummaryDto.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(25.99))
                .build();

        when(orderRepository.findByCustomerId(customerId, pageable)).thenReturn(orderPage);
        when(orderMapper.toSummaryDto(order)).thenReturn(summaryDto);

        Page<OrderSummaryDto> result = orderService.getOrdersByCustomer(customerId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void updateOrderStatus_Success() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        OrderDto updatedDto = OrderDto.builder()
                .id(orderId)
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toDto(any(Order.class))).thenReturn(updatedDto);

        OrderDto result = orderService.updateOrderStatus(orderId, request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw InvalidOrderStateException when trying to change finalized order")
    void updateOrderStatus_FinalizedOrder_ThrowsException() {
        order.setStatus(OrderStatus.DELIVERED);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.IN_DELIVERY);

        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("finalized order");
    }

    @Test
    @DisplayName("Should cancel order successfully when in cancellable state")
    void cancelOrder_Success() {
        order.setStatus(OrderStatus.PENDING);
        CancelOrderRequest request = new CancelOrderRequest();
        request.setReason("Customer changed mind");

        OrderDto cancelledDto = OrderDto.builder()
                .id(orderId)
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toDto(any(Order.class))).thenReturn(cancelledDto);

        OrderDto result = orderService.cancelOrder(orderId, customerId, request);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderEventProducer).sendOrderCancelledEvent(any(Order.class), eq("Customer changed mind"));
    }

    @Test
    @DisplayName("Should throw InvalidOrderStateException when cancelling non-cancellable order")
    void cancelOrder_NonCancellableState_ThrowsException() {
        order.setStatus(OrderStatus.IN_DELIVERY);
        CancelOrderRequest request = new CancelOrderRequest();
        request.setReason("Test");

        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId, request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    @DisplayName("Should handle order accepted event")
    void handleOrderAccepted_Success() {
        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.handleOrderAccepted(orderId, 30);

        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.ACCEPTED_BY_RESTAURANT &&
                savedOrder.getConfirmedAt() != null
        ));
    }

    @Test
    @DisplayName("Should handle order rejected event")
    void handleOrderRejected_Success() {
        Payment payment = Payment.builder()
                .order(order)
                .amount(BigDecimal.valueOf(25.99))
                .status(PaymentStatus.COMPLETED)
                .build();
        order.setPayment(payment);

        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.handleOrderRejected(orderId, "Restaurant busy");

        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.REJECTED &&
                savedOrder.getRejectionReason().equals("Restaurant busy") &&
                savedOrder.getPayment().getStatus() == PaymentStatus.REFUNDED
        ));
    }

    @Test
    @DisplayName("Should handle delivery status change to DELIVERED")
    void handleDeliveryStatusChanged_Delivered() {
        when(orderRepository.findByIdWithItemsAndPayment(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.handleDeliveryStatusChanged(orderId, "DELIVERED");

        verify(orderRepository).save(argThat(savedOrder ->
                savedOrder.getStatus() == OrderStatus.DELIVERED &&
                savedOrder.getDeliveredAt() != null
        ));
    }
}
