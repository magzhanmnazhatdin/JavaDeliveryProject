package com.example.restaurantservice.controller;

import com.example.restaurantservice.dto.menu.MenuItemDto;
import com.example.restaurantservice.dto.restaurant.CreateRestaurantRequest;
import com.example.restaurantservice.dto.restaurant.RestaurantDto;
import com.example.restaurantservice.service.MenuItemService;
import com.example.restaurantservice.service.RestaurantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RestaurantController.class)
class RestaurantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestaurantService restaurantService;

    @MockBean
    private MenuItemService menuItemService;

    @Test
    @DisplayName("Should get all active restaurants without authentication")
    void getRestaurants_NoAuth_Success() throws Exception {
        List<RestaurantDto> restaurants = List.of(
                RestaurantDto.builder()
                        .id(UUID.randomUUID())
                        .name("Pizza Palace")
                        .city("New York")
                        .isActive(true)
                        .build()
        );

        when(restaurantService.getActiveRestaurants()).thenReturn(restaurants);

        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Pizza Palace"));
    }

    @Test
    @DisplayName("Should create restaurant when user is restaurant owner")
    @WithMockUser(roles = "RESTAURANT")
    void createRestaurant_AsRestaurant_Success() throws Exception {
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("Pizza Palace")
                .address("123 Main St")
                .city("New York")
                .build();

        RestaurantDto response = RestaurantDto.builder()
                .id(UUID.randomUUID())
                .name("Pizza Palace")
                .address("123 Main St")
                .city("New York")
                .isActive(true)
                .build();

        when(restaurantService.createRestaurant(any(CreateRestaurantRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/restaurants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pizza Palace"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("Should return 403 when customer tries to create restaurant")
    @WithMockUser(roles = "CUSTOMER")
    void createRestaurant_AsCustomer_Forbidden() throws Exception {
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("Pizza Palace")
                .address("123 Main St")
                .city("New York")
                .build();

        mockMvc.perform(post("/api/restaurants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get restaurant menu without authentication")
    void getRestaurantMenu_NoAuth_Success() throws Exception {
        UUID restaurantId = UUID.randomUUID();
        List<MenuItemDto> menu = List.of(
                MenuItemDto.builder()
                        .id(UUID.randomUUID())
                        .name("Margherita Pizza")
                        .price(BigDecimal.valueOf(12.99))
                        .isAvailable(true)
                        .build()
        );

        when(menuItemService.getAvailableMenuItems(restaurantId)).thenReturn(menu);

        mockMvc.perform(get("/api/restaurants/{id}/menu", restaurantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Margherita Pizza"));
    }

    @Test
    @DisplayName("Should return 400 for invalid request")
    @WithMockUser(roles = "RESTAURANT")
    void createRestaurant_InvalidRequest_BadRequest() throws Exception {
        CreateRestaurantRequest request = CreateRestaurantRequest.builder()
                .name("") // Invalid: empty name
                .address("123 Main St")
                .city("New York")
                .build();

        mockMvc.perform(post("/api/restaurants")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
