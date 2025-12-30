package com.example.restaurantservice.service.impl;

import com.example.restaurantservice.dto.restaurant.CreateRestaurantRequest;
import com.example.restaurantservice.dto.restaurant.RestaurantDto;
import com.example.restaurantservice.dto.restaurant.UpdateRestaurantRequest;
import com.example.restaurantservice.entity.Restaurant;
import com.example.restaurantservice.exception.ConflictException;
import com.example.restaurantservice.exception.ResourceNotFoundException;
import com.example.restaurantservice.mapper.RestaurantMapper;
import com.example.restaurantservice.repository.RestaurantRepository;
import com.example.restaurantservice.service.RestaurantService;
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
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    @Override
    public RestaurantDto createRestaurant(CreateRestaurantRequest request) {
        log.info("Creating restaurant: {}", request.getName());

        Restaurant restaurant = restaurantMapper.toEntity(request);
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant created with ID: {}", savedRestaurant.getId());
        return restaurantMapper.toDto(savedRestaurant);
    }

    @Override
    public RestaurantDto createRestaurantForOwner(CreateRestaurantRequest request, String keycloakId) {
        log.info("Creating restaurant for owner: {}", keycloakId);

        if (restaurantRepository.existsByKeycloakId(keycloakId)) {
            throw new ConflictException("Restaurant already exists for this user");
        }

        Restaurant restaurant = restaurantMapper.toEntity(request);
        restaurant.setKeycloakId(keycloakId);
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant created with ID: {} for owner: {}", savedRestaurant.getId(), keycloakId);
        return restaurantMapper.toDto(savedRestaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDto getRestaurantById(UUID id) {
        log.debug("Getting restaurant by ID: {}", id);
        Restaurant restaurant = findRestaurantById(id);
        return restaurantMapper.toDto(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantDto getRestaurantByKeycloakId(String keycloakId) {
        log.debug("Getting restaurant by Keycloak ID: {}", keycloakId);
        Restaurant restaurant = restaurantRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "keycloakId", keycloakId));
        return restaurantMapper.toDto(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDto> getAllRestaurants() {
        log.debug("Getting all restaurants");
        return restaurantRepository.findAll().stream()
                .map(restaurantMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDto> getActiveRestaurants() {
        log.debug("Getting active restaurants");
        return restaurantRepository.findByIsActiveTrue().stream()
                .map(restaurantMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantDto> getRestaurantsByCity(String city) {
        log.debug("Getting restaurants in city: {}", city);
        return restaurantRepository.findActiveByCity(city).stream()
                .map(restaurantMapper::toDto)
                .toList();
    }

    @Override
    public RestaurantDto updateRestaurant(UUID id, UpdateRestaurantRequest request) {
        log.info("Updating restaurant: {}", id);
        Restaurant restaurant = findRestaurantById(id);

        if (request.getName() != null) {
            restaurant.setName(request.getName());
        }
        if (request.getDescription() != null) {
            restaurant.setDescription(request.getDescription());
        }
        if (request.getAddress() != null) {
            restaurant.setAddress(request.getAddress());
        }
        if (request.getCity() != null) {
            restaurant.setCity(request.getCity());
        }
        if (request.getPhone() != null) {
            restaurant.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            restaurant.setEmail(request.getEmail());
        }
        if (request.getLatitude() != null) {
            restaurant.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            restaurant.setLongitude(request.getLongitude());
        }
        if (request.getOpeningTime() != null) {
            restaurant.setOpeningTime(request.getOpeningTime());
        }
        if (request.getClosingTime() != null) {
            restaurant.setClosingTime(request.getClosingTime());
        }
        if (request.getIsActive() != null) {
            restaurant.setIsActive(request.getIsActive());
        }

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);
        log.info("Restaurant {} updated successfully", id);
        return restaurantMapper.toDto(updatedRestaurant);
    }

    @Override
    public RestaurantDto activateRestaurant(UUID id) {
        log.info("Activating restaurant: {}", id);
        Restaurant restaurant = findRestaurantById(id);
        restaurant.setIsActive(true);
        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);
        return restaurantMapper.toDto(updatedRestaurant);
    }

    @Override
    public RestaurantDto deactivateRestaurant(UUID id) {
        log.info("Deactivating restaurant: {}", id);
        Restaurant restaurant = findRestaurantById(id);
        restaurant.setIsActive(false);
        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);
        return restaurantMapper.toDto(updatedRestaurant);
    }

    @Override
    public void deleteRestaurant(UUID id) {
        log.info("Deleting restaurant: {}", id);
        Restaurant restaurant = findRestaurantById(id);
        restaurantRepository.delete(restaurant);
        log.info("Restaurant {} deleted successfully", id);
    }

    private Restaurant findRestaurantById(UUID id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", id));
    }
}
