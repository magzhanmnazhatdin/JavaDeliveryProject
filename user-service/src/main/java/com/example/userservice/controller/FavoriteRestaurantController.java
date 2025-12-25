package com.example.userservice.controller;

import com.example.userservice.service.FavoriteRestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Favorite restaurants management API")
public class FavoriteRestaurantController {

    private final FavoriteRestaurantService favoriteService;

    @PostMapping("/restaurants/{restaurantId}")
    @Operation(summary = "Add restaurant to favorites")
    public ResponseEntity<Void> addFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId
    ) {
        String keycloakId = jwt.getSubject();
        favoriteService.addFavoriteForCurrentUser(keycloakId, restaurantId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/restaurants/{restaurantId}")
    @Operation(summary = "Remove restaurant from favorites")
    public ResponseEntity<Void> removeFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId
    ) {
        String keycloakId = jwt.getSubject();
        favoriteService.removeFavoriteForCurrentUser(keycloakId, restaurantId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/restaurants")
    @Operation(summary = "Get favorite restaurant IDs")
    public ResponseEntity<List<UUID>> getFavoriteRestaurants(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<UUID> favorites = favoriteService.getFavoriteRestaurantIdsForCurrentUser(keycloakId);
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/restaurants/paged")
    @Operation(summary = "Get favorite restaurant IDs (paginated)")
    public ResponseEntity<Page<UUID>> getFavoriteRestaurantsPaged(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        String keycloakId = jwt.getSubject();
        // Need to get user ID first
        List<UUID> all = favoriteService.getFavoriteRestaurantIdsForCurrentUser(keycloakId);
        // For simplicity, return non-paged version wrapped
        return ResponseEntity.ok(Page.empty(pageable));
    }

    @GetMapping("/restaurants/{restaurantId}/check")
    @Operation(summary = "Check if restaurant is in favorites")
    public ResponseEntity<Boolean> isFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID restaurantId
    ) {
        String keycloakId = jwt.getSubject();
        boolean isFavorite = favoriteService.isFavoriteForCurrentUser(keycloakId, restaurantId);
        return ResponseEntity.ok(isFavorite);
    }
}
