package com.example.restaurantservice.mapper;

import com.example.restaurantservice.dto.menu.CreateMenuItemRequest;
import com.example.restaurantservice.dto.menu.MenuItemDto;
import com.example.restaurantservice.entity.MenuItem;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItemDto toDto(MenuItem menuItem) {
        if (menuItem == null) {
            return null;
        }
        return MenuItemDto.builder()
                .id(menuItem.getId())
                .restaurantId(menuItem.getRestaurant() != null ? menuItem.getRestaurant().getId() : null)
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .category(menuItem.getCategory())
                .imageUrl(menuItem.getImageUrl())
                .isAvailable(menuItem.getIsAvailable())
                .preparationTimeMinutes(menuItem.getPreparationTimeMinutes())
                .createdAt(menuItem.getCreatedAt())
                .updatedAt(menuItem.getUpdatedAt())
                .build();
    }

    public MenuItem toEntity(CreateMenuItemRequest request) {
        if (request == null) {
            return null;
        }
        return MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .preparationTimeMinutes(request.getPreparationTimeMinutes())
                .isAvailable(true)
                .build();
    }
}
