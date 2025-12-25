package com.example.userservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface FavoriteRestaurantService {

    void addFavorite(UUID userId, UUID restaurantId);

    void addFavoriteForCurrentUser(String keycloakId, UUID restaurantId);

    void removeFavorite(UUID userId, UUID restaurantId);

    void removeFavoriteForCurrentUser(String keycloakId, UUID restaurantId);

    List<UUID> getFavoriteRestaurantIds(UUID userId);

    List<UUID> getFavoriteRestaurantIdsForCurrentUser(String keycloakId);

    Page<UUID> getFavoriteRestaurantIds(UUID userId, Pageable pageable);

    boolean isFavorite(UUID userId, UUID restaurantId);

    boolean isFavoriteForCurrentUser(String keycloakId, UUID restaurantId);
}
