package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.restaurant.CreateRestaurantRequest;
import com.example.restaurantservice.dto.restaurant.RestaurantDto;
import com.example.restaurantservice.dto.restaurant.UpdateRestaurantRequest;

import java.util.List;
import java.util.UUID;

public interface RestaurantService {

    RestaurantDto createRestaurant(CreateRestaurantRequest request);

    RestaurantDto createRestaurantForOwner(CreateRestaurantRequest request, String keycloakId);

    RestaurantDto getRestaurantById(UUID id);

    RestaurantDto getRestaurantByKeycloakId(String keycloakId);

    List<RestaurantDto> getAllRestaurants();

    List<RestaurantDto> getActiveRestaurants();

    List<RestaurantDto> getRestaurantsByCity(String city);

    RestaurantDto updateRestaurant(UUID id, UpdateRestaurantRequest request);

    RestaurantDto activateRestaurant(UUID id);

    RestaurantDto deactivateRestaurant(UUID id);

    void deleteRestaurant(UUID id);
}
