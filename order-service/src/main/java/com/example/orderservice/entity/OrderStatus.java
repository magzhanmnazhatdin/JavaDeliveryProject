package com.example.orderservice.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    ACCEPTED_BY_RESTAURANT,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    IN_DELIVERY,
    DELIVERED,
    CANCELLED,
    REJECTED
}
