package com.example.restaurantservice.repository;

import com.example.restaurantservice.entity.RestaurantOrder;
import com.example.restaurantservice.entity.RestaurantOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantOrderRepository extends JpaRepository<RestaurantOrder, UUID> {

    Optional<RestaurantOrder> findByOrderId(UUID orderId);

    List<RestaurantOrder> findByRestaurantId(UUID restaurantId);

    List<RestaurantOrder> findByRestaurantIdAndStatus(UUID restaurantId, RestaurantOrderStatus status);

    @Query("SELECT o FROM RestaurantOrder o WHERE o.restaurant.id = :restaurantId AND o.status IN :statuses ORDER BY o.receivedAt ASC")
    List<RestaurantOrder> findByRestaurantIdAndStatusIn(
            @Param("restaurantId") UUID restaurantId,
            @Param("statuses") List<RestaurantOrderStatus> statuses
    );

    @Query("SELECT o FROM RestaurantOrder o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<RestaurantOrder> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT o FROM RestaurantOrder o LEFT JOIN FETCH o.items WHERE o.orderId = :orderId")
    Optional<RestaurantOrder> findByOrderIdWithItems(@Param("orderId") UUID orderId);

    boolean existsByOrderId(UUID orderId);

    @Query("SELECT COUNT(o) FROM RestaurantOrder o WHERE o.restaurant.id = :restaurantId AND o.status IN ('PENDING', 'ACCEPTED', 'PREPARING')")
    long countActiveOrdersByRestaurantId(@Param("restaurantId") UUID restaurantId);
}
