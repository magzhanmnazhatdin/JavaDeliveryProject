package com.example.orderservice.controller;

import com.example.orderservice.dto.order.*;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderDto> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        OrderDto order = orderService.createOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get order by ID")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (hasRole(jwt, "ADMIN") || hasRole(jwt, "RESTAURANT_OWNER") || hasRole(jwt, "COURIER")) {
            return ResponseEntity.ok(orderService.getOrderById(orderId));
        }
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(orderService.getOrderByIdForCustomer(orderId, customerId));
    }

    @GetMapping("/my-orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user's orders")
    public ResponseEntity<Page<OrderSummaryDto>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId, pageable));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by customer ID (Admin only)")
    public ResponseEntity<Page<OrderSummaryDto>> getOrdersByCustomer(
            @PathVariable UUID customerId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getOrdersByCustomer(customerId, pageable));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @Operation(summary = "Get orders by restaurant ID")
    public ResponseEntity<Page<OrderSummaryDto>> getOrdersByRestaurant(
            @PathVariable UUID restaurantId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurant(restaurantId, pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all orders (Admin only)")
    public ResponseEntity<Page<OrderSummaryDto>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @GetMapping("/restaurant/{restaurantId}/active")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @Operation(summary = "Get active orders for a restaurant")
    public ResponseEntity<List<OrderSummaryDto>> getActiveOrdersByRestaurant(
            @PathVariable UUID restaurantId
    ) {
        return ResponseEntity.ok(orderService.getActiveOrdersByRestaurant(restaurantId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get orders by status (Admin only)")
    public ResponseEntity<Page<OrderSummaryDto>> getOrdersByStatus(
            @PathVariable OrderStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status, pageable));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'COURIER', 'ADMIN')")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request));
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        if (hasRole(jwt, "ADMIN")) {
            return ResponseEntity.ok(orderService.cancelOrderByAdmin(orderId, request));
        }
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(orderService.cancelOrder(orderId, customerId, request));
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update order details (only for pending orders)")
    public ResponseEntity<OrderDto> updateOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateOrderRequest request
    ) {
        if (hasRole(jwt, "ADMIN")) {
            return ResponseEntity.ok(orderService.updateOrderByAdmin(orderId, request));
        }
        UUID customerId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(orderService.updateOrder(orderId, customerId, request));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an order (Admin only, only pending/cancelled/rejected orders)")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    private boolean hasRole(Jwt jwt, String role) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return false;
        }
        var roles = (List<?>) realmAccess.get("roles");
        return roles != null && roles.contains(role);
    }
}
