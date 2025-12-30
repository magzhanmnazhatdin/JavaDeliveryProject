package com.example.restaurantservice.controller;

import com.example.restaurantservice.dto.menu.CreateMenuItemRequest;
import com.example.restaurantservice.dto.menu.MenuItemDto;
import com.example.restaurantservice.dto.restaurant.CreateRestaurantRequest;
import com.example.restaurantservice.dto.restaurant.RestaurantDto;
import com.example.restaurantservice.dto.restaurant.UpdateRestaurantRequest;
import com.example.restaurantservice.service.MenuItemService;
import com.example.restaurantservice.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Restaurant", description = "Restaurant management API")
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final MenuItemService menuItemService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new restaurant", description = "Creates a new restaurant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Restaurant created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "User already has a restaurant")
    })
    public ResponseEntity<RestaurantDto> createRestaurant(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateRestaurantRequest request) {
        log.info("REST request to create restaurant: {}", request.getName());

        // If keycloakId is provided in the request, use it (for API Gateway calls)
        // Otherwise, use the authenticated user's ID
        String keycloakId = request.getKeycloakId();
        if (keycloakId == null || keycloakId.isBlank()) {
            keycloakId = jwt.getSubject();
        }

        RestaurantDto restaurant = restaurantService.createRestaurantForOwner(request, keycloakId);
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurant);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current restaurant", description = "Returns restaurant linked to current user")
    public ResponseEntity<RestaurantDto> getMyRestaurant(@AuthenticationPrincipal Jwt jwt) {
        RestaurantDto restaurant = restaurantService.getRestaurantByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(restaurant);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restaurant by ID", description = "Returns restaurant details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant found"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<RestaurantDto> getRestaurantById(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id) {
        log.debug("REST request to get restaurant: {}", id);
        RestaurantDto restaurant = restaurantService.getRestaurantById(id);
        return ResponseEntity.ok(restaurant);
    }

    @GetMapping
    @Operation(summary = "Get all restaurants", description = "Returns list of restaurants with optional filters")
    public ResponseEntity<List<RestaurantDto>> getRestaurants(
            @Parameter(description = "Filter by city") @RequestParam(required = false) String city,
            @Parameter(description = "Only active restaurants") @RequestParam(defaultValue = "true") boolean activeOnly) {
        log.debug("REST request to get restaurants, city: {}, activeOnly: {}", city, activeOnly);

        List<RestaurantDto> restaurants;
        if (city != null && !city.isBlank()) {
            restaurants = restaurantService.getRestaurantsByCity(city);
        } else if (activeOnly) {
            restaurants = restaurantService.getActiveRestaurants();
        } else {
            restaurants = restaurantService.getAllRestaurants();
        }
        return ResponseEntity.ok(restaurants);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update restaurant", description = "Updates restaurant information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restaurant updated successfully"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<RestaurantDto> updateRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateRestaurantRequest request) {
        log.info("REST request to update restaurant: {}", id);
        RestaurantDto restaurant = restaurantService.updateRestaurant(id, request);
        return ResponseEntity.ok(restaurant);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Activate restaurant", description = "Sets restaurant as active")
    public ResponseEntity<RestaurantDto> activateRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id) {
        log.info("REST request to activate restaurant: {}", id);
        RestaurantDto restaurant = restaurantService.activateRestaurant(id);
        return ResponseEntity.ok(restaurant);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate restaurant", description = "Sets restaurant as inactive")
    public ResponseEntity<RestaurantDto> deactivateRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id) {
        log.info("REST request to deactivate restaurant: {}", id);
        RestaurantDto restaurant = restaurantService.deactivateRestaurant(id);
        return ResponseEntity.ok(restaurant);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete restaurant", description = "Deletes a restaurant (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Restaurant deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Restaurant not found")
    })
    public ResponseEntity<Void> deleteRestaurant(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id) {
        log.info("REST request to delete restaurant: {}", id);
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }

    // Menu endpoints under restaurant
    @GetMapping("/{id}/menu")
    @Operation(summary = "Get restaurant menu", description = "Returns all menu items for a restaurant")
    public ResponseEntity<List<MenuItemDto>> getRestaurantMenu(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id,
            @Parameter(description = "Only available items") @RequestParam(defaultValue = "true") boolean availableOnly) {
        log.debug("REST request to get menu for restaurant: {}", id);
        List<MenuItemDto> menu;
        if (availableOnly) {
            menu = menuItemService.getAvailableMenuItems(id);
        } else {
            menu = menuItemService.getMenuItemsByRestaurant(id);
        }
        return ResponseEntity.ok(menu);
    }

    @GetMapping("/{id}/menu/categories")
    @Operation(summary = "Get menu categories", description = "Returns all categories for a restaurant's menu")
    public ResponseEntity<List<String>> getMenuCategories(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id) {
        log.debug("REST request to get menu categories for restaurant: {}", id);
        List<String> categories = menuItemService.getCategories(id);
        return ResponseEntity.ok(categories);
    }

    @PostMapping("/{id}/menu")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add menu item", description = "Adds a new menu item to the restaurant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Menu item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Menu item with this name already exists")
    })
    public ResponseEntity<MenuItemDto> addMenuItem(
            @Parameter(description = "Restaurant ID") @PathVariable UUID id,
            @Valid @RequestBody CreateMenuItemRequest request) {
        log.info("REST request to add menu item to restaurant: {}", id);
        MenuItemDto menuItem = menuItemService.createMenuItem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(menuItem);
    }

    private boolean hasRole(Jwt jwt, String role) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return false;
        }
        var roles = (List<?>) realmAccess.get("roles");
        return roles != null && roles.contains(role);
    }
}
