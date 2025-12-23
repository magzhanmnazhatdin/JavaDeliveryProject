package com.example.restaurantservice.controller;

import com.example.restaurantservice.dto.order.AcceptOrderRequest;
import com.example.restaurantservice.dto.order.RejectOrderRequest;
import com.example.restaurantservice.dto.order.RestaurantOrderDto;
import com.example.restaurantservice.entity.RestaurantOrderStatus;
import com.example.restaurantservice.service.RestaurantOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurant-orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restaurant Orders", description = "Restaurant order management API")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('RESTAURANT', 'ADMIN')")
public class RestaurantOrderController {

    private final RestaurantOrderService orderService;

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Returns order details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<RestaurantOrderDto> getOrderById(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        log.debug("REST request to get restaurant order: {}", id);
        RestaurantOrderDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/by-order/{orderId}")
    @Operation(summary = "Get order by original order ID", description = "Returns order details by the original order ID")
    public ResponseEntity<RestaurantOrderDto> getOrderByOrderId(
            @Parameter(description = "Original Order ID") @PathVariable UUID orderId) {
        log.debug("REST request to get restaurant order by orderId: {}", orderId);
        RestaurantOrderDto order = orderService.getOrderByOrderId(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/restaurant/{restaurantId}")
    @Operation(summary = "Get orders by restaurant", description = "Returns all orders for a restaurant")
    public ResponseEntity<List<RestaurantOrderDto>> getOrdersByRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable UUID restaurantId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) RestaurantOrderStatus status) {
        log.debug("REST request to get orders for restaurant: {}, status: {}", restaurantId, status);
        List<RestaurantOrderDto> orders;
        if (status != null) {
            orders = orderService.getOrdersByRestaurantAndStatus(restaurantId, status);
        } else {
            orders = orderService.getOrdersByRestaurant(restaurantId);
        }
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/restaurant/{restaurantId}/pending")
    @Operation(summary = "Get pending orders", description = "Returns all pending orders for a restaurant")
    public ResponseEntity<List<RestaurantOrderDto>> getPendingOrders(
            @Parameter(description = "Restaurant ID") @PathVariable UUID restaurantId) {
        log.debug("REST request to get pending orders for restaurant: {}", restaurantId);
        List<RestaurantOrderDto> orders = orderService.getPendingOrders(restaurantId);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/restaurant/{restaurantId}/active")
    @Operation(summary = "Get active orders", description = "Returns all active orders (pending, accepted, preparing)")
    public ResponseEntity<List<RestaurantOrderDto>> getActiveOrders(
            @Parameter(description = "Restaurant ID") @PathVariable UUID restaurantId) {
        log.debug("REST request to get active orders for restaurant: {}", restaurantId);
        List<RestaurantOrderDto> orders = orderService.getActiveOrders(restaurantId);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept order", description = "Accepts an order and sets estimated prep time")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order accepted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<RestaurantOrderDto> acceptOrder(
            @Parameter(description = "Order ID") @PathVariable UUID id,
            @Valid @RequestBody(required = false) AcceptOrderRequest request) {
        log.info("REST request to accept order: {}", id);
        RestaurantOrderDto order = orderService.acceptOrder(id, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject order", description = "Rejects an order with a reason")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition or missing reason"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<RestaurantOrderDto> rejectOrder(
            @Parameter(description = "Order ID") @PathVariable UUID id,
            @Valid @RequestBody RejectOrderRequest request) {
        log.info("REST request to reject order: {}", id);
        RestaurantOrderDto order = orderService.rejectOrder(id, request);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/start-preparing")
    @Operation(summary = "Start preparing order", description = "Marks order as being prepared")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<RestaurantOrderDto> startPreparing(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        log.info("REST request to start preparing order: {}", id);
        RestaurantOrderDto order = orderService.startPreparing(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/ready")
    @Operation(summary = "Mark order as ready", description = "Marks order as ready for pickup")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order marked as ready"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<RestaurantOrderDto> markAsReady(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        log.info("REST request to mark order as ready: {}", id);
        RestaurantOrderDto order = orderService.markAsReady(id);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{id}/picked-up")
    @Operation(summary = "Mark order as picked up", description = "Marks order as picked up by courier")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order marked as picked up"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<RestaurantOrderDto> markAsPickedUp(
            @Parameter(description = "Order ID") @PathVariable UUID id) {
        log.info("REST request to mark order as picked up: {}", id);
        RestaurantOrderDto order = orderService.markAsPickedUp(id);
        return ResponseEntity.ok(order);
    }
}
