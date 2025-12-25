package com.example.deliveryservice.controller;

import com.example.deliveryservice.dto.delivery.*;
import com.example.deliveryservice.entity.DeliveryStatus;
import com.example.deliveryservice.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Delivery", description = "Delivery management API")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    @Operation(summary = "Create a new delivery", description = "Creates a new delivery for an order")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Delivery created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Delivery for this order already exists")
    })
    public ResponseEntity<DeliveryDto> createDelivery(@Valid @RequestBody CreateDeliveryRequest request) {
        log.info("REST request to create delivery for order: {}", request.getOrderId());
        DeliveryDto delivery = deliveryService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(delivery);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get delivery by ID", description = "Returns delivery details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery found"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<DeliveryDto> getDeliveryById(
            @Parameter(description = "Delivery ID") @PathVariable UUID id) {
        log.debug("REST request to get delivery: {}", id);
        DeliveryDto delivery = deliveryService.getDeliveryById(id);
        return ResponseEntity.ok(delivery);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get delivery by order ID", description = "Returns delivery details for a specific order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery found"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<DeliveryDto> getDeliveryByOrderId(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {
        log.debug("REST request to get delivery by order: {}", orderId);
        DeliveryDto delivery = deliveryService.getDeliveryByOrderId(orderId);
        return ResponseEntity.ok(delivery);
    }

    @GetMapping("/courier/{courierId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER')")
    @Operation(summary = "Get deliveries by courier", description = "Returns all deliveries assigned to a courier")
    public ResponseEntity<List<DeliveryDto>> getDeliveriesByCourier(
            @Parameter(description = "Courier ID") @PathVariable UUID courierId) {
        log.debug("REST request to get deliveries for courier: {}", courierId);
        List<DeliveryDto> deliveries = deliveryService.getDeliveriesByCourierId(courierId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get deliveries by customer", description = "Returns all deliveries for a customer")
    public ResponseEntity<List<DeliveryDto>> getDeliveriesByCustomer(
            @Parameter(description = "Customer ID") @PathVariable UUID customerId) {
        log.debug("REST request to get deliveries for customer: {}", customerId);
        List<DeliveryDto> deliveries = deliveryService.getDeliveriesByCustomerId(customerId);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    @Operation(summary = "Get deliveries by status", description = "Returns all deliveries with a specific status")
    public ResponseEntity<List<DeliveryDto>> getDeliveriesByStatus(
            @Parameter(description = "Delivery status") @PathVariable DeliveryStatus status) {
        log.debug("REST request to get deliveries by status: {}", status);
        List<DeliveryDto> deliveries = deliveryService.getDeliveriesByStatus(status);
        return ResponseEntity.ok(deliveries);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all deliveries", description = "Returns all deliveries (Admin only)")
    public ResponseEntity<List<DeliveryDto>> getAllDeliveries() {
        log.debug("REST request to get all deliveries");
        List<DeliveryDto> deliveries = deliveryService.getAllDeliveries();
        return ResponseEntity.ok(deliveries);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign courier to delivery", description = "Manually assigns a courier to a delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Courier not available or delivery not in correct status"),
            @ApiResponse(responseCode = "404", description = "Delivery or courier not found")
    })
    public ResponseEntity<DeliveryDto> assignCourier(
            @Parameter(description = "Delivery ID") @PathVariable UUID id,
            @Valid @RequestBody AssignCourierRequest request) {
        log.info("REST request to assign courier {} to delivery {}", request.getCourierId(), id);
        DeliveryDto delivery = deliveryService.assignCourier(id, request);
        return ResponseEntity.ok(delivery);
    }

    @PostMapping("/{id}/assign-auto")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Auto-assign courier", description = "Automatically assigns an available courier to a delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier assigned successfully"),
            @ApiResponse(responseCode = "400", description = "No available couriers or delivery not in correct status"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<DeliveryDto> assignCourierAutomatically(
            @Parameter(description = "Delivery ID") @PathVariable UUID id) {
        log.info("REST request to auto-assign courier to delivery {}", id);
        DeliveryDto delivery = deliveryService.assignCourierAutomatically(id);
        return ResponseEntity.ok(delivery);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER')")
    @Operation(summary = "Update delivery status", description = "Updates the delivery status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<DeliveryDto> updateDeliveryStatus(
            @Parameter(description = "Delivery ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryStatusRequest request) {
        log.info("REST request to update delivery {} status to {}", id, request.getStatus());
        DeliveryDto delivery = deliveryService.updateDeliveryStatus(id, request);
        return ResponseEntity.ok(delivery);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESTAURANT')")
    @Operation(summary = "Update delivery details", description = "Updates delivery address and notes")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Delivery updated successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot update delivery in current status"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<DeliveryDto> updateDelivery(
            @Parameter(description = "Delivery ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateDeliveryRequest request) {
        log.info("REST request to update delivery {}", id);
        DeliveryDto delivery = deliveryService.updateDelivery(id, request);
        return ResponseEntity.ok(delivery);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete delivery", description = "Deletes a pending or cancelled delivery (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Delivery deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete delivery in current status"),
            @ApiResponse(responseCode = "404", description = "Delivery not found")
    })
    public ResponseEntity<Void> deleteDelivery(
            @Parameter(description = "Delivery ID") @PathVariable UUID id) {
        log.info("REST request to delete delivery {}", id);
        deliveryService.deleteDelivery(id);
        return ResponseEntity.noContent().build();
    }
}
