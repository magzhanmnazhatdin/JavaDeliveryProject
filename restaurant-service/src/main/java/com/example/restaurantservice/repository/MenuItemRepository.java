package com.example.restaurantservice.repository;

import com.example.restaurantservice.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantId(UUID restaurantId);

    List<MenuItem> findByRestaurantIdAndIsAvailableTrue(UUID restaurantId);

    List<MenuItem> findByRestaurantIdAndCategory(UUID restaurantId, String category);

    @Query("SELECT DISTINCT m.category FROM MenuItem m WHERE m.restaurant.id = :restaurantId AND m.category IS NOT NULL")
    List<String> findCategoriesByRestaurantId(@Param("restaurantId") UUID restaurantId);

    @Query("SELECT m FROM MenuItem m WHERE m.restaurant.id = :restaurantId AND m.isAvailable = true ORDER BY m.category, m.name")
    List<MenuItem> findAvailableMenuItemsSorted(@Param("restaurantId") UUID restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(UUID restaurantId, String name);
}
