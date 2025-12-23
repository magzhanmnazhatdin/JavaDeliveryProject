package com.example.deliveryservice.service;

import com.example.deliveryservice.dto.courier.*;
import com.example.deliveryservice.entity.Courier;
import com.example.deliveryservice.entity.CourierStatus;

import java.util.List;
import java.util.UUID;

public interface CourierService {

    CourierDto createCourier(CreateCourierRequest request);

    CourierDto getCourierById(UUID id);

    CourierDto getCourierByKeycloakId(String keycloakId);

    List<CourierDto> getAllCouriers();

    List<CourierDto> getAvailableCouriers();

    List<CourierDto> getCouriersByStatus(CourierStatus status);

    CourierDto updateCourier(UUID id, UpdateCourierRequest request);

    CourierDto updateCourierStatus(UUID id, UpdateCourierStatusRequest request);

    CourierDto updateCourierLocation(UUID id, UpdateCourierLocationRequest request);

    void deleteCourier(UUID id);

    Courier findAvailableCourierForAssignment();
}
