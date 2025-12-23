package com.example.deliveryservice.repository;

import com.example.deliveryservice.entity.Delivery;
import com.example.deliveryservice.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);

    List<Delivery> findByCourierId(UUID courierId);

    List<Delivery> findByCustomerId(UUID customerId);

    List<Delivery> findByStatus(DeliveryStatus status);

    @Query("SELECT d FROM Delivery d WHERE d.courier.id = :courierId AND d.status IN :statuses")
    List<Delivery> findByCourierIdAndStatusIn(
            @Param("courierId") UUID courierId,
            @Param("statuses") List<DeliveryStatus> statuses
    );

    @Query("SELECT d FROM Delivery d LEFT JOIN FETCH d.courier WHERE d.id = :id")
    Optional<Delivery> findByIdWithCourier(@Param("id") UUID id);

    @Query("SELECT d FROM Delivery d LEFT JOIN FETCH d.courier WHERE d.orderId = :orderId")
    Optional<Delivery> findByOrderIdWithCourier(@Param("orderId") UUID orderId);

    boolean existsByOrderId(UUID orderId);

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.courier.id = :courierId AND d.status NOT IN ('DELIVERED', 'CANCELLED')")
    long countActiveDeliveriesByCourierId(@Param("courierId") UUID courierId);
}
