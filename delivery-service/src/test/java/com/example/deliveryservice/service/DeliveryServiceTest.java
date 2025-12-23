package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.delivery.CreateDeliveryRequest;
import com.example.deliveryservice.dto.delivery.DeliveryDto;
import com.example.deliveryservice.dto.delivery.UpdateDeliveryStatusRequest;
import com.example.deliveryservice.entity.Courier;
import com.example.deliveryservice.entity.CourierStatus;
import com.example.deliveryservice.entity.Delivery;
import com.example.deliveryservice.entity.DeliveryStatus;
import com.example.deliveryservice.exception.BadRequestException;
import com.example.deliveryservice.exception.ConflictException;
import com.example.deliveryservice.exception.ResourceNotFoundException;
import com.example.deliveryservice.kafka.DeliveryEventProducer;
import com.example.deliveryservice.mapper.DeliveryMapper;
import com.example.deliveryservice.repository.CourierRepository;
import com.example.deliveryservice.repository.DeliveryRepository;
import com.example.deliveryservice.service.impl.DeliveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private CourierService courierService;

    @Mock
    private DeliveryMapper deliveryMapper;

    @Mock
    private DeliveryEventProducer eventProducer;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    private Delivery delivery;
    private DeliveryDto deliveryDto;
    private Courier courier;
    private CreateDeliveryRequest createRequest;

    @BeforeEach
    void setUp() {
        courier = Courier.builder()
                .id(UUID.randomUUID())
                .name("John Courier")
                .phone("+1234567890")
                .status(CourierStatus.AVAILABLE)
                .build();

        delivery = Delivery.builder()
                .id(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .deliveryAddress("123 Main St")
                .status(DeliveryStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        deliveryDto = DeliveryDto.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .customerId(delivery.getCustomerId())
                .restaurantId(delivery.getRestaurantId())
                .deliveryAddress(delivery.getDeliveryAddress())
                .status(delivery.getStatus())
                .build();

        createRequest = CreateDeliveryRequest.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .restaurantId(UUID.randomUUID())
                .deliveryAddress("123 Main St")
                .build();
    }

    @Test
    @DisplayName("Should create delivery successfully")
    void createDelivery_Success() {
        when(deliveryRepository.existsByOrderId(createRequest.getOrderId())).thenReturn(false);
        when(deliveryMapper.toEntity(createRequest)).thenReturn(delivery);
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);
        when(deliveryMapper.toDto(delivery)).thenReturn(deliveryDto);
        when(courierService.findAvailableCourierForAssignment()).thenReturn(null);

        DeliveryDto result = deliveryService.createDelivery(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        verify(deliveryRepository).save(any(Delivery.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when delivery for order already exists")
    void createDelivery_OrderExists_ThrowsConflict() {
        when(deliveryRepository.existsByOrderId(createRequest.getOrderId())).thenReturn(true);

        assertThatThrownBy(() -> deliveryService.createDelivery(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get delivery by order ID")
    void getDeliveryByOrderId_Success() {
        when(deliveryRepository.findByOrderIdWithCourier(delivery.getOrderId()))
                .thenReturn(Optional.of(delivery));
        when(deliveryMapper.toDto(delivery)).thenReturn(deliveryDto);

        DeliveryDto result = deliveryService.getDeliveryByOrderId(delivery.getOrderId());

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(delivery.getOrderId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when delivery not found")
    void getDeliveryByOrderId_NotFound() {
        UUID orderId = UUID.randomUUID();
        when(deliveryRepository.findByOrderIdWithCourier(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.getDeliveryByOrderId(orderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Delivery");
    }

    @Test
    @DisplayName("Should update delivery status from COURIER_ASSIGNED to PICKED_UP")
    void updateDeliveryStatus_ToPickedUp_Success() {
        delivery.setStatus(DeliveryStatus.COURIER_ASSIGNED);
        delivery.setCourier(courier);

        UpdateDeliveryStatusRequest request = UpdateDeliveryStatusRequest.builder()
                .status(DeliveryStatus.PICKED_UP)
                .build();

        DeliveryDto updatedDto = DeliveryDto.builder()
                .id(delivery.getId())
                .status(DeliveryStatus.PICKED_UP)
                .build();

        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any(Delivery.class))).thenReturn(delivery);
        when(deliveryMapper.toDto(any(Delivery.class))).thenReturn(updatedDto);

        DeliveryDto result = deliveryService.updateDeliveryStatus(delivery.getId(), request);

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        verify(eventProducer).sendDeliveryStatusChangedEvent(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException for invalid status transition")
    void updateDeliveryStatus_InvalidTransition_ThrowsBadRequest() {
        delivery.setStatus(DeliveryStatus.DELIVERED);

        UpdateDeliveryStatusRequest request = UpdateDeliveryStatusRequest.builder()
                .status(DeliveryStatus.PICKED_UP)
                .build();

        when(deliveryRepository.findById(delivery.getId())).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(delivery.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid status transition");
    }
}
