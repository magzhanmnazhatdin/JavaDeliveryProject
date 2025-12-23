package com.example.restaurantservice.repository;

import com.example.restaurantservice.entity.RestaurantOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RestaurantOrderItemRepository extends JpaRepository<RestaurantOrderItem, UUID> {

    List<RestaurantOrderItem> findByRestaurantOrderId(UUID restaurantOrderId);
}
