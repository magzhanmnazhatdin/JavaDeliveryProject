package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.restaurant.CreateRestaurantRequest;
import com.example.restaurantservice.dto.restaurant.RestaurantDto;
import com.example.restaurantservice.dto.restaurant.UpdateRestaurantRequest;
import com.example.restaurantservice.entity.Restaurant;
import com.example.restaurantservice.exception.ResourceNotFoundException;
import com.example.restaurantservice.mapper.RestaurantMapper;
import com.example.restaurantservice.repository.RestaurantRepository;
import com.example.restaurantservice.service.impl.RestaurantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantMapper restaurantMapper;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private Restaurant restaurant;
    private RestaurantDto restaurantDto;
    private CreateRestaurantRequest createRequest;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Pizza Palace")
                .description("Best pizza in town")
                .address("123 Main St")
                .city("New York")
                .phone("+1234567890")
                .isActive(true)
                .averageRating(BigDecimal.valueOf(4.5))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        restaurantDto = RestaurantDto.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .description(restaurant.getDescription())
                .address(restaurant.getAddress())
                .city(restaurant.getCity())
                .isActive(true)
                .build();

        createRequest = CreateRestaurantRequest.builder()
                .name("Pizza Palace")
                .description("Best pizza in town")
                .address("123 Main St")
                .city("New York")
                .phone("+1234567890")
                .build();
    }

    @Test
    @DisplayName("Should create restaurant successfully")
    void createRestaurant_Success() {
        when(restaurantMapper.toEntity(createRequest)).thenReturn(restaurant);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);
        when(restaurantMapper.toDto(restaurant)).thenReturn(restaurantDto);

        RestaurantDto result = restaurantService.createRestaurant(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Pizza Palace");
        verify(restaurantRepository).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Should get restaurant by ID")
    void getRestaurantById_Success() {
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(restaurantMapper.toDto(restaurant)).thenReturn(restaurantDto);

        RestaurantDto result = restaurantService.getRestaurantById(restaurant.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(restaurant.getId());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when restaurant not found")
    void getRestaurantById_NotFound() {
        UUID id = UUID.randomUUID();
        when(restaurantRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> restaurantService.getRestaurantById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Restaurant");
    }

    @Test
    @DisplayName("Should return active restaurants")
    void getActiveRestaurants_Success() {
        List<Restaurant> restaurants = List.of(restaurant);
        when(restaurantRepository.findByIsActiveTrue()).thenReturn(restaurants);
        when(restaurantMapper.toDto(restaurant)).thenReturn(restaurantDto);

        List<RestaurantDto> result = restaurantService.getActiveRestaurants();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should update restaurant")
    void updateRestaurant_Success() {
        UpdateRestaurantRequest updateRequest = UpdateRestaurantRequest.builder()
                .name("New Name")
                .build();

        Restaurant updatedRestaurant = Restaurant.builder()
                .id(restaurant.getId())
                .name("New Name")
                .isActive(true)
                .build();

        RestaurantDto updatedDto = RestaurantDto.builder()
                .id(restaurant.getId())
                .name("New Name")
                .build();

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(updatedRestaurant);
        when(restaurantMapper.toDto(updatedRestaurant)).thenReturn(updatedDto);

        RestaurantDto result = restaurantService.updateRestaurant(restaurant.getId(), updateRequest);

        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("Should deactivate restaurant")
    void deactivateRestaurant_Success() {
        RestaurantDto inactiveDto = RestaurantDto.builder()
                .id(restaurant.getId())
                .isActive(false)
                .build();

        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);
        when(restaurantMapper.toDto(any(Restaurant.class))).thenReturn(inactiveDto);

        RestaurantDto result = restaurantService.deactivateRestaurant(restaurant.getId());

        assertThat(result.getIsActive()).isFalse();
    }
}
