package com.example.orderservice.repository;

import com.example.orderservice.entity.Payment;
import com.example.orderservice.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(UUID orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM Payment p JOIN p.order o WHERE o.customerId = :customerId")
    Page<Payment> findByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    boolean existsByOrderId(UUID orderId);
}
