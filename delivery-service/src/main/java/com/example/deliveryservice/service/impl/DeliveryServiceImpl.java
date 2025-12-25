package com.example.deliveryservice.service.impl;

import com.example.deliveryservice.dto.delivery.*;
import com.example.deliveryservice.dto.event.CourierAssignedEvent;
import com.example.deliveryservice.dto.event.DeliveryStatusChangedEvent;
import com.example.deliveryservice.dto.event.OrderAcceptedEvent;
import com.example.deliveryservice.entity.Courier;
import com.example.deliveryservice.entity.CourierStatus;
import com.example.deliveryservice.entity.Delivery;
import com.example.deliveryservice.entity.DeliveryStatus;
import com.example.deliveryservice.exception.BadRequestException;
import com.example.deliveryservice.exception.ConflictException;
import com.example.deliveryservice.exception.ResourceNotFoundException;
import com.example.deliveryservice.mapper.DeliveryMapper;
import com.example.deliveryservice.repository.CourierRepository;
import com.example.deliveryservice.repository.DeliveryRepository;
import com.example.deliveryservice.service.CourierService;
import com.example.deliveryservice.service.DeliveryService;
import com.example.deliveryservice.kafka.DeliveryEventProducer;
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
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final CourierRepository courierRepository;
    private final CourierService courierService;
    private final DeliveryMapper deliveryMapper;
    private final DeliveryEventProducer eventProducer;

    @Override
    public DeliveryDto createDelivery(CreateDeliveryRequest request) {
        log.info("Creating delivery for order: {}", request.getOrderId());

        if (deliveryRepository.existsByOrderId(request.getOrderId())) {
            throw new ConflictException("Delivery for order " + request.getOrderId() + " already exists");
        }

        Delivery delivery = deliveryMapper.toEntity(request);
        Delivery savedDelivery = deliveryRepository.save(delivery);

        log.info("Delivery created with ID: {} for order: {}", savedDelivery.getId(), request.getOrderId());

        // Try to assign courier automatically
        tryAssignCourierAutomatically(savedDelivery);

        return deliveryMapper.toDto(savedDelivery);
    }

    @Override
    public DeliveryDto createDeliveryFromOrderAccepted(OrderAcceptedEvent event) {
        log.info("Creating delivery from OrderAcceptedEvent for order: {}", event.getOrderId());

        if (deliveryRepository.existsByOrderId(event.getOrderId())) {
            log.warn("Delivery for order {} already exists, skipping", event.getOrderId());
            return deliveryMapper.toDto(
                    deliveryRepository.findByOrderId(event.getOrderId()).orElseThrow()
            );
        }

        Delivery delivery = deliveryMapper.fromOrderAcceptedEvent(event);
        Delivery savedDelivery = deliveryRepository.save(delivery);

        log.info("Delivery created with ID: {} from event for order: {}", savedDelivery.getId(), event.getOrderId());

        // Try to assign courier automatically
        tryAssignCourierAutomatically(savedDelivery);

        return deliveryMapper.toDto(savedDelivery);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryById(UUID id) {
        log.debug("Getting delivery by ID: {}", id);
        Delivery delivery = deliveryRepository.findByIdWithCourier(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", id));
        return deliveryMapper.toDto(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryDto getDeliveryByOrderId(UUID orderId) {
        log.debug("Getting delivery by order ID: {}", orderId);
        Delivery delivery = deliveryRepository.findByOrderIdWithCourier(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "orderId", orderId));
        return deliveryMapper.toDto(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> getDeliveriesByCourierId(UUID courierId) {
        log.debug("Getting deliveries for courier: {}", courierId);
        return deliveryRepository.findByCourierId(courierId).stream()
                .map(deliveryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> getDeliveriesByCustomerId(UUID customerId) {
        log.debug("Getting deliveries for customer: {}", customerId);
        return deliveryRepository.findByCustomerId(customerId).stream()
                .map(deliveryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> getDeliveriesByStatus(DeliveryStatus status) {
        log.debug("Getting deliveries by status: {}", status);
        return deliveryRepository.findByStatus(status).stream()
                .map(deliveryMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> getAllDeliveries() {
        log.debug("Getting all deliveries");
        return deliveryRepository.findAll().stream()
                .map(deliveryMapper::toDto)
                .toList();
    }

    @Override
    public DeliveryDto assignCourier(UUID deliveryId, AssignCourierRequest request) {
        log.info("Assigning courier {} to delivery {}", request.getCourierId(), deliveryId);

        Delivery delivery = findDeliveryById(deliveryId);
        validateDeliveryForAssignment(delivery);

        Courier courier = courierRepository.findById(request.getCourierId())
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", request.getCourierId()));

        if (courier.getStatus() != CourierStatus.AVAILABLE) {
            throw new BadRequestException("Courier is not available for assignment");
        }

        assignCourierToDelivery(delivery, courier);

        return deliveryMapper.toDto(delivery);
    }

    @Override
    public DeliveryDto assignCourierAutomatically(UUID deliveryId) {
        log.info("Auto-assigning courier to delivery {}", deliveryId);

        Delivery delivery = findDeliveryById(deliveryId);
        validateDeliveryForAssignment(delivery);

        Courier courier = courierService.findAvailableCourierForAssignment();
        if (courier == null) {
            throw new BadRequestException("No available couriers for assignment");
        }

        assignCourierToDelivery(delivery, courier);

        return deliveryMapper.toDto(delivery);
    }

    @Override
    public DeliveryDto updateDeliveryStatus(UUID deliveryId, UpdateDeliveryStatusRequest request) {
        log.info("Updating delivery {} status to {}", deliveryId, request.getStatus());

        Delivery delivery = findDeliveryById(deliveryId);
        DeliveryStatus previousStatus = delivery.getStatus();

        validateStatusTransition(delivery, request.getStatus());

        delivery.setStatus(request.getStatus());

        if (request.getCourierNotes() != null) {
            delivery.setCourierNotes(request.getCourierNotes());
        }

        // Set timestamps based on new status
        switch (request.getStatus()) {
            case PICKED_UP -> delivery.setPickedUpAt(Instant.now());
            case DELIVERED -> {
                delivery.setDeliveredAt(Instant.now());
                // Release courier
                if (delivery.getCourier() != null) {
                    delivery.getCourier().setStatus(CourierStatus.AVAILABLE);
                    courierRepository.save(delivery.getCourier());
                }
            }
            case CANCELLED -> {
                delivery.setCancelledAt(Instant.now());
                delivery.setCancellationReason(request.getCancellationReason());
                // Release courier
                if (delivery.getCourier() != null) {
                    delivery.getCourier().setStatus(CourierStatus.AVAILABLE);
                    courierRepository.save(delivery.getCourier());
                }
            }
            default -> { }
        }

        Delivery updatedDelivery = deliveryRepository.save(delivery);

        // Publish status changed event
        DeliveryStatusChangedEvent event = DeliveryStatusChangedEvent.builder()
                .eventType("DeliveryStatusChanged")
                .deliveryId(updatedDelivery.getId())
                .orderId(updatedDelivery.getOrderId())
                .courierId(updatedDelivery.getCourier() != null ? updatedDelivery.getCourier().getId() : null)
                .previousStatus(previousStatus)
                .newStatus(request.getStatus())
                .notes(request.getCourierNotes())
                .changedAt(Instant.now())
                .build();

        eventProducer.sendDeliveryStatusChangedEvent(event);

        log.info("Delivery {} status updated from {} to {}", deliveryId, previousStatus, request.getStatus());
        return deliveryMapper.toDto(updatedDelivery);
    }

    @Override
    public DeliveryDto updateDelivery(UUID deliveryId, UpdateDeliveryRequest request) {
        log.info("Updating delivery: {}", deliveryId);

        Delivery delivery = findDeliveryById(deliveryId);

        if (delivery.getStatus() == DeliveryStatus.DELIVERED ||
            delivery.getStatus() == DeliveryStatus.CANCELLED) {
            throw new BadRequestException(
                    "Cannot update delivery in status: " + delivery.getStatus()
            );
        }

        if (request.getDeliveryAddress() != null) {
            delivery.setDeliveryAddress(request.getDeliveryAddress());
        }
        if (request.getDeliveryLat() != null) {
            delivery.setDeliveryLat(request.getDeliveryLat());
        }
        if (request.getDeliveryLng() != null) {
            delivery.setDeliveryLng(request.getDeliveryLng());
        }
        if (request.getCustomerNotes() != null) {
            delivery.setCustomerNotes(request.getCustomerNotes());
        }

        Delivery updatedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery {} updated successfully", deliveryId);

        return deliveryMapper.toDto(updatedDelivery);
    }

    @Override
    public void deleteDelivery(UUID deliveryId) {
        log.info("Deleting delivery: {}", deliveryId);

        Delivery delivery = findDeliveryById(deliveryId);

        if (delivery.getStatus() != DeliveryStatus.PENDING &&
            delivery.getStatus() != DeliveryStatus.CANCELLED) {
            throw new BadRequestException(
                    "Only pending or cancelled deliveries can be deleted. Current status: " + delivery.getStatus()
            );
        }

        if (delivery.getCourier() != null) {
            delivery.getCourier().setStatus(CourierStatus.AVAILABLE);
            courierRepository.save(delivery.getCourier());
        }

        deliveryRepository.delete(delivery);
        log.info("Delivery {} deleted", deliveryId);
    }

    @Override
    public void handleOrderReady(UUID orderId) {
        log.info("Handling order ready for order: {}", orderId);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "orderId", orderId));

        if (delivery.getStatus() == DeliveryStatus.COURIER_ASSIGNED) {
            // Notify courier that order is ready for pickup
            log.info("Order {} is ready, courier {} can pick up", orderId,
                    delivery.getCourier() != null ? delivery.getCourier().getId() : "not assigned");
        }
    }

    private void tryAssignCourierAutomatically(Delivery delivery) {
        Courier courier = courierService.findAvailableCourierForAssignment();
        if (courier != null) {
            assignCourierToDelivery(delivery, courier);
            log.info("Courier {} auto-assigned to delivery {}", courier.getId(), delivery.getId());
        } else {
            log.warn("No available couriers for automatic assignment to delivery {}", delivery.getId());
        }
    }

    private void assignCourierToDelivery(Delivery delivery, Courier courier) {
        delivery.setCourier(courier);
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setAssignedAt(Instant.now());

        courier.setStatus(CourierStatus.BUSY);
        courierRepository.save(courier);
        deliveryRepository.save(delivery);

        // Publish courier assigned event
        CourierAssignedEvent event = CourierAssignedEvent.builder()
                .eventType("CourierAssigned")
                .deliveryId(delivery.getId())
                .orderId(delivery.getOrderId())
                .courierId(courier.getId())
                .courierName(courier.getName())
                .courierPhone(courier.getPhone())
                .assignedAt(delivery.getAssignedAt())
                .build();

        eventProducer.sendCourierAssignedEvent(event);

        log.info("Courier {} assigned to delivery {}", courier.getId(), delivery.getId());
    }

    private void validateDeliveryForAssignment(Delivery delivery) {
        if (delivery.getCourier() != null) {
            throw new BadRequestException("Delivery already has a courier assigned");
        }
        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new BadRequestException("Delivery must be in PENDING status for courier assignment");
        }
    }

    private void validateStatusTransition(Delivery delivery, DeliveryStatus newStatus) {
        DeliveryStatus currentStatus = delivery.getStatus();

        boolean validTransition = switch (currentStatus) {
            case PENDING -> newStatus == DeliveryStatus.COURIER_ASSIGNED || newStatus == DeliveryStatus.CANCELLED;
            case COURIER_ASSIGNED -> newStatus == DeliveryStatus.PICKED_UP || newStatus == DeliveryStatus.CANCELLED;
            case PICKED_UP -> newStatus == DeliveryStatus.IN_TRANSIT || newStatus == DeliveryStatus.CANCELLED;
            case IN_TRANSIT -> newStatus == DeliveryStatus.DELIVERED || newStatus == DeliveryStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!validTransition) {
            throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus)
            );
        }
    }

    private Delivery findDeliveryById(UUID id) {
        return deliveryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", "id", id));
    }
}
