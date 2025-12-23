package com.example.restaurantservice.dto.restaurant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantDto {
    private UUID id;
    private String name;
    private String description;
    private String address;
    private String city;
    private String phone;
    private String email;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Boolean isActive;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Instant createdAt;
    private Instant updatedAt;
}
