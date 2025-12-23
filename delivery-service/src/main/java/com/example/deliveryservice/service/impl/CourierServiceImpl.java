package com.example.deliveryservice.service.impl;

import com.example.deliveryservice.dto.courier.*;
import com.example.deliveryservice.entity.Courier;
import com.example.deliveryservice.entity.CourierStatus;
import com.example.deliveryservice.exception.BadRequestException;
import com.example.deliveryservice.exception.ConflictException;
import com.example.deliveryservice.exception.ResourceNotFoundException;
import com.example.deliveryservice.mapper.CourierMapper;
import com.example.deliveryservice.repository.CourierRepository;
import com.example.deliveryservice.service.CourierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CourierServiceImpl implements CourierService {

    private final CourierRepository courierRepository;
    private final CourierMapper courierMapper;

    @Override
    public CourierDto createCourier(CreateCourierRequest request) {
        log.info("Creating courier with name: {}", request.getName());

        if (courierRepository.existsByPhone(request.getPhone())) {
            throw new ConflictException("Courier with phone " + request.getPhone() + " already exists");
        }

        Courier courier = courierMapper.toEntity(request);
        Courier savedCourier = courierRepository.save(courier);

        log.info("Courier created with ID: {}", savedCourier.getId());
        return courierMapper.toDto(savedCourier);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierDto getCourierById(UUID id) {
        log.debug("Getting courier by ID: {}", id);
        Courier courier = findCourierById(id);
        return courierMapper.toDto(courier);
    }

    @Override
    @Transactional(readOnly = true)
    public CourierDto getCourierByKeycloakId(String keycloakId) {
        log.debug("Getting courier by Keycloak ID: {}", keycloakId);
        Courier courier = courierRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "keycloakId", keycloakId));
        return courierMapper.toDto(courier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierDto> getAllCouriers() {
        log.debug("Getting all couriers");
        return courierRepository.findAll().stream()
                .map(courierMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierDto> getAvailableCouriers() {
        log.debug("Getting available couriers");
        return courierRepository.findAvailableCouriers().stream()
                .map(courierMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourierDto> getCouriersByStatus(CourierStatus status) {
        log.debug("Getting couriers by status: {}", status);
        return courierRepository.findByStatus(status).stream()
                .map(courierMapper::toDto)
                .toList();
    }

    @Override
    public CourierDto updateCourier(UUID id, UpdateCourierRequest request) {
        log.info("Updating courier: {}", id);
        Courier courier = findCourierById(id);

        if (request.getName() != null) {
            courier.setName(request.getName());
        }
        if (request.getPhone() != null) {
            if (!courier.getPhone().equals(request.getPhone()) &&
                    courierRepository.existsByPhone(request.getPhone())) {
                throw new ConflictException("Courier with phone " + request.getPhone() + " already exists");
            }
            courier.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            courier.setEmail(request.getEmail());
        }

        Courier updatedCourier = courierRepository.save(courier);
        log.info("Courier {} updated successfully", id);
        return courierMapper.toDto(updatedCourier);
    }

    @Override
    public CourierDto updateCourierStatus(UUID id, UpdateCourierStatusRequest request) {
        log.info("Updating courier {} status to: {}", id, request.getStatus());
        Courier courier = findCourierById(id);
        courier.setStatus(request.getStatus());
        Courier updatedCourier = courierRepository.save(courier);
        log.info("Courier {} status updated to {}", id, request.getStatus());
        return courierMapper.toDto(updatedCourier);
    }

    @Override
    public CourierDto updateCourierLocation(UUID id, UpdateCourierLocationRequest request) {
        log.debug("Updating courier {} location", id);
        Courier courier = findCourierById(id);
        courier.setCurrentLocationLat(request.getLatitude());
        courier.setCurrentLocationLng(request.getLongitude());
        Courier updatedCourier = courierRepository.save(courier);
        return courierMapper.toDto(updatedCourier);
    }

    @Override
    public void deleteCourier(UUID id) {
        log.info("Deleting courier: {}", id);
        Courier courier = findCourierById(id);

        if (courier.getStatus() == CourierStatus.BUSY) {
            throw new BadRequestException("Cannot delete courier who is currently busy with a delivery");
        }

        courierRepository.delete(courier);
        log.info("Courier {} deleted successfully", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Courier findAvailableCourierForAssignment() {
        log.debug("Finding available courier for assignment");
        return courierRepository.findFirstAvailableCourier()
                .orElse(null);
    }

    private Courier findCourierById(UUID id) {
        return courierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Courier", "id", id));
    }
}
