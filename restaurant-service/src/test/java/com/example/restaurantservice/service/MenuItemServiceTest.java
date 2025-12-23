package com.example.restaurantservice.service;

import com.example.restaurantservice.dto.menu.CreateMenuItemRequest;
import com.example.restaurantservice.dto.menu.MenuItemDto;
import com.example.restaurantservice.entity.MenuItem;
import com.example.restaurantservice.entity.Restaurant;
import com.example.restaurantservice.exception.ConflictException;
import com.example.restaurantservice.exception.ResourceNotFoundException;
import com.example.restaurantservice.mapper.MenuItemMapper;
import com.example.restaurantservice.repository.MenuItemRepository;
import com.example.restaurantservice.repository.RestaurantRepository;
import com.example.restaurantservice.service.impl.MenuItemServiceImpl;
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
class MenuItemServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private MenuItemMapper menuItemMapper;

    @InjectMocks
    private MenuItemServiceImpl menuItemService;

    private Restaurant restaurant;
    private MenuItem menuItem;
    private MenuItemDto menuItemDto;
    private CreateMenuItemRequest createRequest;

    @BeforeEach
    void setUp() {
        restaurant = Restaurant.builder()
                .id(UUID.randomUUID())
                .name("Pizza Palace")
                .isActive(true)
                .build();

        menuItem = MenuItem.builder()
                .id(UUID.randomUUID())
                .restaurant(restaurant)
                .name("Margherita Pizza")
                .description("Classic pizza")
                .price(BigDecimal.valueOf(12.99))
                .category("Pizza")
                .isAvailable(true)
                .preparationTimeMinutes(20)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        menuItemDto = MenuItemDto.builder()
                .id(menuItem.getId())
                .restaurantId(restaurant.getId())
                .name(menuItem.getName())
                .price(menuItem.getPrice())
                .category(menuItem.getCategory())
                .isAvailable(true)
                .build();

        createRequest = CreateMenuItemRequest.builder()
                .name("Margherita Pizza")
                .description("Classic pizza")
                .price(BigDecimal.valueOf(12.99))
                .category("Pizza")
                .build();
    }

    @Test
    @DisplayName("Should create menu item successfully")
    void createMenuItem_Success() {
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.existsByRestaurantIdAndNameIgnoreCase(restaurant.getId(), createRequest.getName())).thenReturn(false);
        when(menuItemMapper.toEntity(createRequest)).thenReturn(menuItem);
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);
        when(menuItemMapper.toDto(menuItem)).thenReturn(menuItemDto);

        MenuItemDto result = menuItemService.createMenuItem(restaurant.getId(), createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Margherita Pizza");
        verify(menuItemRepository).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when menu item name exists")
    void createMenuItem_NameExists_ThrowsConflict() {
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.existsByRestaurantIdAndNameIgnoreCase(restaurant.getId(), createRequest.getName())).thenReturn(true);

        assertThatThrownBy(() -> menuItemService.createMenuItem(restaurant.getId(), createRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(menuItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get available menu items")
    void getAvailableMenuItems_Success() {
        List<MenuItem> items = List.of(menuItem);
        when(menuItemRepository.findAvailableMenuItemsSorted(restaurant.getId())).thenReturn(items);
        when(menuItemMapper.toDto(menuItem)).thenReturn(menuItemDto);

        List<MenuItemDto> result = menuItemService.getAvailableMenuItems(restaurant.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should set menu item availability")
    void setAvailability_Success() {
        MenuItemDto unavailableDto = MenuItemDto.builder()
                .id(menuItem.getId())
                .isAvailable(false)
                .build();

        when(menuItemRepository.findById(menuItem.getId())).thenReturn(Optional.of(menuItem));
        when(menuItemRepository.save(any(MenuItem.class))).thenReturn(menuItem);
        when(menuItemMapper.toDto(any(MenuItem.class))).thenReturn(unavailableDto);

        MenuItemDto result = menuItemService.setAvailability(menuItem.getId(), false);

        assertThat(result.getIsAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when menu item not found")
    void getMenuItemById_NotFound() {
        UUID id = UUID.randomUUID();
        when(menuItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.getMenuItemById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MenuItem");
    }
}
