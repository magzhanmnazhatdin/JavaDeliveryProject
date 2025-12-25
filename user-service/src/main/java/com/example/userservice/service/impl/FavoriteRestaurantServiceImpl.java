package com.example.userservice.service.impl;

import com.example.userservice.entity.FavoriteRestaurant;
import com.example.userservice.entity.User;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.repository.FavoriteRestaurantRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.FavoriteRestaurantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FavoriteRestaurantServiceImpl implements FavoriteRestaurantService {

    private final FavoriteRestaurantRepository favoriteRepository;
    private final UserRepository userRepository;

    @Override
    public void addFavorite(UUID userId, UUID restaurantId) {
        User user = findUserById(userId);

        if (favoriteRepository.existsByUserIdAndRestaurantId(userId, restaurantId)) {
            log.debug("Restaurant {} is already a favorite for user {}", restaurantId, userId);
            return;
        }

        FavoriteRestaurant favorite = FavoriteRestaurant.builder()
                .user(user)
                .restaurantId(restaurantId)
                .build();

        favoriteRepository.save(favorite);
        log.info("Restaurant {} added to favorites for user {}", restaurantId, userId);
    }

    @Override
    public void addFavoriteForCurrentUser(String keycloakId, UUID restaurantId) {
        User user = findUserByKeycloakId(keycloakId);
        addFavorite(user.getId(), restaurantId);
    }

    @Override
    public void removeFavorite(UUID userId, UUID restaurantId) {
        favoriteRepository.deleteByUserIdAndRestaurantId(userId, restaurantId);
        log.info("Restaurant {} removed from favorites for user {}", restaurantId, userId);
    }

    @Override
    public void removeFavoriteForCurrentUser(String keycloakId, UUID restaurantId) {
        User user = findUserByKeycloakId(keycloakId);
        removeFavorite(user.getId(), restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getFavoriteRestaurantIds(UUID userId) {
        return favoriteRepository.findByUserId(userId).stream()
                .map(FavoriteRestaurant::getRestaurantId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getFavoriteRestaurantIdsForCurrentUser(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return getFavoriteRestaurantIds(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UUID> getFavoriteRestaurantIds(UUID userId, Pageable pageable) {
        return favoriteRepository.findByUserId(userId, pageable)
                .map(FavoriteRestaurant::getRestaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, UUID restaurantId) {
        return favoriteRepository.existsByUserIdAndRestaurantId(userId, restaurantId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavoriteForCurrentUser(String keycloakId, UUID restaurantId) {
        User user = findUserByKeycloakId(keycloakId);
        return isFavorite(user.getId(), restaurantId);
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private User findUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found with keycloak ID: " + keycloakId));
    }
}
