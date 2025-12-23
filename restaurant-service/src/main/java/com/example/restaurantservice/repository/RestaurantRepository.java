package com.example.restaurantservice.repository;

import com.example.restaurantservice.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    Optional<Restaurant> findByKeycloakId(String keycloakId);

    List<Restaurant> findByCityIgnoreCase(String city);

    List<Restaurant> findByIsActiveTrue();

    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true AND LOWER(r.city) = LOWER(:city)")
    List<Restaurant> findActiveByCity(@Param("city") String city);

    @Query("SELECT r FROM Restaurant r LEFT JOIN FETCH r.menuItems WHERE r.id = :id")
    Optional<Restaurant> findByIdWithMenuItems(@Param("id") UUID id);

    @Query("SELECT DISTINCT r FROM Restaurant r LEFT JOIN FETCH r.menuItems WHERE r.isActive = true")
    List<Restaurant> findAllActiveWithMenuItems();

    boolean existsByKeycloakId(String keycloakId);

    @Query("SELECT r FROM Restaurant r WHERE r.isActive = true ORDER BY r.averageRating DESC")
    List<Restaurant> findTopRatedRestaurants();
}
