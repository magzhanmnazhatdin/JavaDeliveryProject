package com.example.restaurantservice.service.impl;

import com.example.restaurantservice.dto.menu.CreateMenuItemRequest;
import com.example.restaurantservice.dto.menu.MenuItemDto;
import com.example.restaurantservice.dto.menu.UpdateMenuItemRequest;
import com.example.restaurantservice.entity.MenuItem;
import com.example.restaurantservice.entity.Restaurant;
import com.example.restaurantservice.exception.ConflictException;
import com.example.restaurantservice.exception.ResourceNotFoundException;
import com.example.restaurantservice.mapper.MenuItemMapper;
import com.example.restaurantservice.repository.MenuItemRepository;
import com.example.restaurantservice.repository.RestaurantRepository;
import com.example.restaurantservice.service.MenuItemService;
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
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemMapper menuItemMapper;

    @Override
    public MenuItemDto createMenuItem(UUID restaurantId, CreateMenuItemRequest request) {
        log.info("Creating menu item '{}' for restaurant {}", request.getName(), restaurantId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", "id", restaurantId));

        if (menuItemRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, request.getName())) {
            throw new ConflictException("Menu item with name '" + request.getName() + "' already exists in this restaurant");
        }

        MenuItem menuItem = menuItemMapper.toEntity(request);
        menuItem.setRestaurant(restaurant);
        MenuItem savedItem = menuItemRepository.save(menuItem);

        log.info("Menu item created with ID: {}", savedItem.getId());
        return menuItemMapper.toDto(savedItem);
    }

    @Override
    @Transactional(readOnly = true)
    public MenuItemDto getMenuItemById(UUID id) {
        log.debug("Getting menu item by ID: {}", id);
        MenuItem menuItem = findMenuItemById(id);
        return menuItemMapper.toDto(menuItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemDto> getMenuItemsByRestaurant(UUID restaurantId) {
        log.debug("Getting menu items for restaurant: {}", restaurantId);
        return menuItemRepository.findByRestaurantId(restaurantId).stream()
                .map(menuItemMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemDto> getAvailableMenuItems(UUID restaurantId) {
        log.debug("Getting available menu items for restaurant: {}", restaurantId);
        return menuItemRepository.findAvailableMenuItemsSorted(restaurantId).stream()
                .map(menuItemMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemDto> getMenuItemsByCategory(UUID restaurantId, String category) {
        log.debug("Getting menu items by category '{}' for restaurant: {}", category, restaurantId);
        return menuItemRepository.findByRestaurantIdAndCategory(restaurantId, category).stream()
                .map(menuItemMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories(UUID restaurantId) {
        log.debug("Getting categories for restaurant: {}", restaurantId);
        return menuItemRepository.findCategoriesByRestaurantId(restaurantId);
    }

    @Override
    public MenuItemDto updateMenuItem(UUID id, UpdateMenuItemRequest request) {
        log.info("Updating menu item: {}", id);
        MenuItem menuItem = findMenuItemById(id);

        if (request.getName() != null) {
            menuItem.setName(request.getName());
        }
        if (request.getDescription() != null) {
            menuItem.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            menuItem.setPrice(request.getPrice());
        }
        if (request.getCategory() != null) {
            menuItem.setCategory(request.getCategory());
        }
        if (request.getImageUrl() != null) {
            menuItem.setImageUrl(request.getImageUrl());
        }
        if (request.getIsAvailable() != null) {
            menuItem.setIsAvailable(request.getIsAvailable());
        }
        if (request.getPreparationTimeMinutes() != null) {
            menuItem.setPreparationTimeMinutes(request.getPreparationTimeMinutes());
        }

        MenuItem updatedItem = menuItemRepository.save(menuItem);
        log.info("Menu item {} updated successfully", id);
        return menuItemMapper.toDto(updatedItem);
    }

    @Override
    public MenuItemDto setAvailability(UUID id, boolean available) {
        log.info("Setting menu item {} availability to {}", id, available);
        MenuItem menuItem = findMenuItemById(id);
        menuItem.setIsAvailable(available);
        MenuItem updatedItem = menuItemRepository.save(menuItem);
        return menuItemMapper.toDto(updatedItem);
    }

    @Override
    public void deleteMenuItem(UUID id) {
        log.info("Deleting menu item: {}", id);
        MenuItem menuItem = findMenuItemById(id);
        menuItemRepository.delete(menuItem);
        log.info("Menu item {} deleted successfully", id);
    }

    private MenuItem findMenuItemById(UUID id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", "id", id));
    }
}
