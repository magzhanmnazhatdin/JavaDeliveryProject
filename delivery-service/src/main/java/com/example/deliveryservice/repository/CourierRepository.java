package com.example.deliveryservice.repository;

import com.example.deliveryservice.entity.Courier;
import com.example.deliveryservice.entity.CourierStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CourierRepository extends JpaRepository<Courier, UUID> {

    Optional<Courier> findByKeycloakId(String keycloakId);

    List<Courier> findByStatus(CourierStatus status);

    @Query("SELECT c FROM Courier c WHERE c.status = 'AVAILABLE' ORDER BY c.updatedAt ASC")
    List<Courier> findAvailableCouriers();

    @Query("SELECT c FROM Courier c WHERE c.status = 'AVAILABLE' ORDER BY c.updatedAt ASC LIMIT 1")
    Optional<Courier> findFirstAvailableCourier();

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByPhone(String phone);
}
