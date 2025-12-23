package com.example.deliveryservice.mapper;

import com.example.deliveryservice.dto.courier.CourierDto;
import com.example.deliveryservice.dto.courier.CreateCourierRequest;
import com.example.deliveryservice.entity.Courier;
import com.example.deliveryservice.entity.CourierStatus;
import org.springframework.stereotype.Component;

@Component
public class CourierMapper {

    public CourierDto toDto(Courier courier) {
        if (courier == null) {
            return null;
        }
        return CourierDto.builder()
                .id(courier.getId())
                .name(courier.getName())
                .phone(courier.getPhone())
                .email(courier.getEmail())
                .status(courier.getStatus())
                .currentLocationLat(courier.getCurrentLocationLat())
                .currentLocationLng(courier.getCurrentLocationLng())
                .createdAt(courier.getCreatedAt())
                .updatedAt(courier.getUpdatedAt())
                .build();
    }

    public Courier toEntity(CreateCourierRequest request) {
        if (request == null) {
            return null;
        }
        return Courier.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(CourierStatus.AVAILABLE)
                .build();
    }
}
