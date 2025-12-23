package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.menu.CreateMenuItemRequest;
import com.example.restaurantservice.dto.menu.MenuItemDto;
import com.example.restaurantservice.dto.menu.UpdateMenuItemRequest;

import java.util.List;
import java.util.UUID;

public interface MenuItemService {

    MenuItemDto createMenuItem(UUID restaurantId, CreateMenuItemRequest request);

    MenuItemDto getMenuItemById(UUID id);

    List<MenuItemDto> getMenuItemsByRestaurant(UUID restaurantId);

    List<MenuItemDto> getAvailableMenuItems(UUID restaurantId);

    List<MenuItemDto> getMenuItemsByCategory(UUID restaurantId, String category);

    List<String> getCategories(UUID restaurantId);

    MenuItemDto updateMenuItem(UUID id, UpdateMenuItemRequest request);

    MenuItemDto setAvailability(UUID id, boolean available);

    void deleteMenuItem(UUID id);
}
