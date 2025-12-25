package com.example.userservice.repository;

import com.example.userservice.entity.FavoriteRestaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRestaurantRepository extends JpaRepository<FavoriteRestaurant, UUID> {

    List<FavoriteRestaurant> findByUserId(UUID userId);

    Page<FavoriteRestaurant> findByUserId(UUID userId, Pageable pageable);

    Optional<FavoriteRestaurant> findByUserIdAndRestaurantId(UUID userId, UUID restaurantId);

    boolean existsByUserIdAndRestaurantId(UUID userId, UUID restaurantId);

    void deleteByUserIdAndRestaurantId(UUID userId, UUID restaurantId);

    long countByUserId(UUID userId);
}
