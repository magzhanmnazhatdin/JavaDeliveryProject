package com.example.restaurantservice.controller;

import com.example.restaurantservice.dto.menu.MenuItemDto;
import com.example.restaurantservice.dto.menu.UpdateMenuItemRequest;
import com.example.restaurantservice.service.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/menu-items")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Menu Items", description = "Menu item management API")
@SecurityRequirement(name = "bearerAuth")
public class MenuItemController {

    private final MenuItemService menuItemService;

    @GetMapping("/{id}")
    @Operation(summary = "Get menu item by ID", description = "Returns menu item details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu item found"),
            @ApiResponse(responseCode = "404", description = "Menu item not found")
    })
    public ResponseEntity<MenuItemDto> getMenuItemById(
            @Parameter(description = "Menu item ID") @PathVariable UUID id) {
        log.debug("REST request to get menu item: {}", id);
        MenuItemDto menuItem = menuItemService.getMenuItemById(id);
        return ResponseEntity.ok(menuItem);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @Operation(summary = "Update menu item", description = "Updates menu item information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Menu item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Menu item not found")
    })
    public ResponseEntity<MenuItemDto> updateMenuItem(
            @Parameter(description = "Menu item ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateMenuItemRequest request) {
        log.info("REST request to update menu item: {}", id);
        MenuItemDto menuItem = menuItemService.updateMenuItem(id, request);
        return ResponseEntity.ok(menuItem);
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @Operation(summary = "Set menu item availability", description = "Sets menu item as available or unavailable")
    public ResponseEntity<MenuItemDto> setAvailability(
            @Parameter(description = "Menu item ID") @PathVariable UUID id,
            @Parameter(description = "Availability status") @RequestParam boolean available) {
        log.info("REST request to set menu item {} availability to {}", id, available);
        MenuItemDto menuItem = menuItemService.setAvailability(id, available);
        return ResponseEntity.ok(menuItem);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @Operation(summary = "Delete menu item", description = "Deletes a menu item")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Menu item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Menu item not found")
    })
    public ResponseEntity<Void> deleteMenuItem(
            @Parameter(description = "Menu item ID") @PathVariable UUID id) {
        log.info("REST request to delete menu item: {}", id);
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }
}
