package com.example.deliveryservice.dto.courier;

import com.example.deliveryservice.entity.CourierStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourierDto {
    private UUID id;
    private String name;
    private String phone;
    private String email;
    private CourierStatus status;
    private BigDecimal currentLocationLat;
    private BigDecimal currentLocationLng;
    private Instant createdAt;
    private Instant updatedAt;
}
