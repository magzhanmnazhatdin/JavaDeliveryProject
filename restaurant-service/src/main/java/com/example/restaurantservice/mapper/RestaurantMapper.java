package com.example.restaurantservice.mapper;

import com.example.restaurantservice.dto.restaurant.CreateRestaurantRequest;
import com.example.restaurantservice.dto.restaurant.RestaurantDto;
import com.example.restaurantservice.entity.Restaurant;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMapper {

    public RestaurantDto toDto(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }
        return RestaurantDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .city(restaurant.getCity())
                .phone(restaurant.getPhone())
                .email(restaurant.getEmail())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .isActive(restaurant.getIsActive())
                .openingTime(restaurant.getOpeningTime())
                .closingTime(restaurant.getClosingTime())
                .averageRating(restaurant.getAverageRating())
                .totalReviews(restaurant.getTotalReviews())
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }

    public Restaurant toEntity(CreateRestaurantRequest request) {
        if (request == null) {
            return null;
        }
        return Restaurant.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .phone(request.getPhone())
                .email(request.getEmail())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .isActive(true)
                .build();
    }
}
