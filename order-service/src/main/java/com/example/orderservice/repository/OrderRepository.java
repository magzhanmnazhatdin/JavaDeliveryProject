package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByRestaurantId(UUID restaurantId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status, Pageable pageable);

    Page<Order> findByRestaurantIdAndStatus(UUID restaurantId, OrderStatus status, Pageable pageable);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    @Query("SELECT o FROM Order o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByCustomer(@Param("customerId") UUID customerId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.restaurantId = :restaurantId AND o.status IN :statuses ORDER BY o.createdAt ASC")
    List<Order> findActiveOrdersByRestaurant(
            @Param("restaurantId") UUID restaurantId,
            @Param("statuses") List<OrderStatus> statuses
    );

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") UUID id);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items LEFT JOIN FETCH o.payment WHERE o.id = :id")
    Optional<Order> findByIdWithItemsAndPayment(@Param("id") UUID id);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.customerId = :customerId AND o.status = :status")
    long countByCustomerIdAndStatus(@Param("customerId") UUID customerId, @Param("status") OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :start AND :end")
    List<Order> findOrdersCreatedBetween(@Param("start") Instant start, @Param("end") Instant end);

    boolean existsByCustomerIdAndStatusIn(UUID customerId, List<OrderStatus> statuses);
}
