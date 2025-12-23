package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.courier.CourierDto;
import com.example.deliveryservice.dto.courier.CreateCourierRequest;
import com.example.deliveryservice.dto.courier.UpdateCourierStatusRequest;
import com.example.deliveryservice.entity.Courier;
import com.example.deliveryservice.entity.CourierStatus;
import com.example.deliveryservice.exception.ConflictException;
import com.example.deliveryservice.exception.ResourceNotFoundException;
import com.example.deliveryservice.mapper.CourierMapper;
import com.example.deliveryservice.repository.CourierRepository;
import com.example.deliveryservice.service.impl.CourierServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierServiceTest {

    @Mock
    private CourierRepository courierRepository;

    @Mock
    private CourierMapper courierMapper;

    @InjectMocks
    private CourierServiceImpl courierService;

    private Courier courier;
    private CourierDto courierDto;
    private CreateCourierRequest createRequest;

    @BeforeEach
    void setUp() {
        courier = Courier.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .phone("+1234567890")
                .email("john@example.com")
                .status(CourierStatus.AVAILABLE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        courierDto = CourierDto.builder()
                .id(courier.getId())
                .name(courier.getName())
                .phone(courier.getPhone())
                .email(courier.getEmail())
                .status(courier.getStatus())
                .build();

        createRequest = CreateCourierRequest.builder()
                .name("John Doe")
                .phone("+1234567890")
                .email("john@example.com")
                .build();
    }

    @Test
    @DisplayName("Should create courier successfully")
    void createCourier_Success() {
        when(courierRepository.existsByPhone(createRequest.getPhone())).thenReturn(false);
        when(courierMapper.toEntity(createRequest)).thenReturn(courier);
        when(courierRepository.save(any(Courier.class))).thenReturn(courier);
        when(courierMapper.toDto(courier)).thenReturn(courierDto);

        CourierDto result = courierService.createCourier(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getPhone()).isEqualTo("+1234567890");
        verify(courierRepository).save(any(Courier.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when phone already exists")
    void createCourier_PhoneExists_ThrowsConflict() {
        when(courierRepository.existsByPhone(createRequest.getPhone())).thenReturn(true);

        assertThatThrownBy(() -> courierService.createCourier(createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(courierRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get courier by ID")
    void getCourierById_Success() {
        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierMapper.toDto(courier)).thenReturn(courierDto);

        CourierDto result = courierService.getCourierById(courier.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(courier.getId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when courier not found")
    void getCourierById_NotFound() {
        UUID id = UUID.randomUUID();
        when(courierRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courierService.getCourierById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Courier");
    }

    @Test
    @DisplayName("Should return available couriers")
    void getAvailableCouriers_Success() {
        List<Courier> couriers = List.of(courier);
        when(courierRepository.findAvailableCouriers()).thenReturn(couriers);
        when(courierMapper.toDto(courier)).thenReturn(courierDto);

        List<CourierDto> result = courierService.getAvailableCouriers();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(CourierStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Should update courier status")
    void updateCourierStatus_Success() {
        UpdateCourierStatusRequest request = new UpdateCourierStatusRequest(CourierStatus.BUSY);
        Courier updatedCourier = Courier.builder()
                .id(courier.getId())
                .name(courier.getName())
                .phone(courier.getPhone())
                .status(CourierStatus.BUSY)
                .build();
        CourierDto updatedDto = CourierDto.builder()
                .id(courier.getId())
                .status(CourierStatus.BUSY)
                .build();

        when(courierRepository.findById(courier.getId())).thenReturn(Optional.of(courier));
        when(courierRepository.save(any(Courier.class))).thenReturn(updatedCourier);
        when(courierMapper.toDto(updatedCourier)).thenReturn(updatedDto);

        CourierDto result = courierService.updateCourierStatus(courier.getId(), request);

        assertThat(result.getStatus()).isEqualTo(CourierStatus.BUSY);
    }
}
