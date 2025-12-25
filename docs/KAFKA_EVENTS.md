# Kafka Event Flow Documentation

## Overview

The system uses Apache Kafka for asynchronous, event-driven communication between microservices. This enables loose coupling and eventual consistency.

## Kafka Configuration

| Property | Value |
|----------|-------|
| Bootstrap Servers | localhost:9092 (local), kafka:29092 (docker) |
| Zookeeper | localhost:2181 |
| Kafka UI | http://localhost:8090 |

---

## Topics

| Topic | Description | Producers | Consumers |
|-------|-------------|-----------|-----------|
| `order-events` | Order lifecycle events | Order Service | Restaurant Service, Delivery Service |
| `payment-events` | Payment status events | Order Service | - |
| `user-events` | User profile events | User Service | - |
| `restaurant-events` | Restaurant order events | Restaurant Service | Delivery Service |
| `delivery-events` | Delivery status events | Delivery Service | - |

---

## Event Definitions

### Order Events (`order-events`)

```java
public class OrderEvent {
    private String eventId;
    private String eventType;  // ORDER_CREATED, ORDER_CANCELLED
    private Long orderId;
    private String customerId;
    private String restaurantId;
    private BigDecimal totalPrice;
    private String deliveryAddress;
    private List<OrderItemDto> items;
    private LocalDateTime timestamp;
}
```

**Event Types:**
- `ORDER_CREATED` - New order placed by customer
- `ORDER_CANCELLED` - Order cancelled by customer/system

### Payment Events (`payment-events`)

```java
public class PaymentEvent {
    private String eventId;
    private String eventType;  // PAYMENT_COMPLETED, PAYMENT_FAILED
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionId;
    private LocalDateTime timestamp;
}
```

**Event Types:**
- `PAYMENT_COMPLETED` - Payment successfully processed
- `PAYMENT_FAILED` - Payment processing failed

### User Events (`user-events`)

```java
public class UserEvent {
    private String eventId;
    private String eventType;  // USER_CREATED, USER_UPDATED, USER_STATUS_CHANGED
    private Long userId;
    private String keycloakId;
    private String email;
    private String role;
    private String status;
    private LocalDateTime timestamp;
}
```

**Event Types:**
- `USER_CREATED` - New user registered
- `USER_UPDATED` - User profile updated
- `USER_STATUS_CHANGED` - User status changed (ACTIVE, SUSPENDED, etc.)

### Restaurant Events (`restaurant-events`)

```java
public class RestaurantOrderEvent {
    private String eventId;
    private String eventType;  // ORDER_ACCEPTED, ORDER_REJECTED, ORDER_READY
    private Long restaurantOrderId;
    private String orderId;
    private String restaurantId;
    private Integer estimatedPrepTime;
    private String reason;  // for rejection
    private LocalDateTime timestamp;
}
```

**Event Types:**
- `ORDER_ACCEPTED` - Restaurant accepted the order
- `ORDER_REJECTED` - Restaurant rejected the order
- `ORDER_READY` - Food is ready for pickup

### Delivery Events (`delivery-events`)

```java
public class DeliveryEvent {
    private String eventId;
    private String eventType;  // COURIER_ASSIGNED, DELIVERY_STATUS_CHANGED
    private Long deliveryId;
    private String orderId;
    private Long courierId;
    private String status;
    private Integer estimatedTime;
    private LocalDateTime timestamp;
}
```

**Event Types:**
- `COURIER_ASSIGNED` - Courier assigned to delivery
- `DELIVERY_STATUS_CHANGED` - Delivery status updated

---

## Event Flow Diagrams

### Complete Order Flow

```mermaid
sequenceDiagram
    participant C as Customer
    participant OS as Order Service
    participant K as Kafka
    participant RS as Restaurant Service
    participant DS as Delivery Service
    participant CR as Courier

    C->>OS: 1. Create Order
    OS->>OS: Save Order (PENDING)
    OS->>K: 2. Publish ORDER_CREATED

    K-->>RS: 3. Consume ORDER_CREATED
    RS->>RS: Create RestaurantOrder

    alt Restaurant Accepts
        RS->>K: 4a. Publish ORDER_ACCEPTED
        K-->>OS: Update order status
        K-->>DS: 5. Consume ORDER_ACCEPTED
        DS->>DS: Create Delivery (PENDING)

        RS->>RS: Prepare Food
        RS->>K: 6. Publish ORDER_READY
        K-->>DS: 7. Consume ORDER_READY

        DS->>DS: 8. Auto-assign Courier
        DS->>K: 9. Publish COURIER_ASSIGNED

        CR->>DS: 10. Pickup Order
        DS->>K: 11. Publish DELIVERY_STATUS_CHANGED (PICKED_UP)

        CR->>DS: 12. Deliver Order
        DS->>K: 13. Publish DELIVERY_STATUS_CHANGED (DELIVERED)

    else Restaurant Rejects
        RS->>K: 4b. Publish ORDER_REJECTED
        K-->>OS: Update order status (CANCELLED)
    end
```

### Payment Flow

```mermaid
sequenceDiagram
    participant C as Customer
    participant OS as Order Service
    participant K as Kafka
    participant PS as Payment Provider

    C->>OS: 1. Process Payment
    OS->>PS: 2. Submit Payment

    alt Payment Success
        PS-->>OS: 3a. Payment Approved
        OS->>OS: Update Payment (COMPLETED)
        OS->>K: 4a. Publish PAYMENT_COMPLETED
        OS-->>C: 5a. Payment Successful
    else Payment Failed
        PS-->>OS: 3b. Payment Declined
        OS->>OS: Update Payment (FAILED)
        OS->>K: 4b. Publish PAYMENT_FAILED
        OS-->>C: 5b. Payment Failed
    end
```

### User Registration Flow

```mermaid
sequenceDiagram
    participant U as User
    participant KC as Keycloak
    participant US as User Service
    participant K as Kafka

    U->>KC: 1. Register Account
    KC->>KC: Create User
    KC-->>U: 2. Registration Success

    U->>US: 3. Complete Profile
    US->>US: Save User
    US->>K: 4. Publish USER_CREATED
    US-->>U: 5. Profile Created
```

---

## Kafka Producers

### Order Service Producer

```java
@Service
@RequiredArgsConstructor
public class OrderEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(Order order) {
        OrderEvent event = OrderEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("ORDER_CREATED")
            .orderId(order.getId())
            .customerId(order.getCustomerId())
            .restaurantId(order.getRestaurantId())
            .totalPrice(order.getTotalPrice())
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send("order-events", event);
    }
}
```

### User Service Producer

```java
@Service
@RequiredArgsConstructor
public class UserEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserCreatedEvent(User user) {
        UserEvent event = UserEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("USER_CREATED")
            .userId(user.getId())
            .keycloakId(user.getKeycloakId())
            .email(user.getEmail())
            .role(user.getRole().name())
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send("user-events", event);
    }
}
```

---

## Kafka Consumers

### Restaurant Service Consumer

```java
@Service
@RequiredArgsConstructor
public class OrderEventsListener {
    private final RestaurantOrderService restaurantOrderService;

    @KafkaListener(topics = "order-events", groupId = "restaurant-service")
    public void handleOrderEvent(OrderEvent event) {
        if ("ORDER_CREATED".equals(event.getEventType())) {
            restaurantOrderService.createRestaurantOrder(event);
        }
    }
}
```

### Delivery Service Consumer

```java
@Service
@RequiredArgsConstructor
public class OrderEventsListener {
    private final DeliveryService deliveryService;

    @KafkaListener(topics = "order-events", groupId = "delivery-service")
    public void handleOrderEvent(OrderEvent event) {
        if ("ORDER_ACCEPTED".equals(event.getEventType())) {
            deliveryService.createDelivery(event);
        } else if ("ORDER_READY".equals(event.getEventType())) {
            deliveryService.assignCourier(event.getOrderId());
        }
    }
}
```

---

## Consumer Groups

| Consumer Group | Topics | Purpose |
|----------------|--------|---------|
| `restaurant-service` | order-events | Process incoming orders |
| `delivery-service` | order-events, restaurant-events | Manage deliveries |

---

## Error Handling

### Dead Letter Queue (DLQ)

For failed message processing:

```yaml
spring:
  kafka:
    consumer:
      properties:
        spring.deserializer.value.delegate.class: org.springframework.kafka.support.serializer.JsonDeserializer
    listener:
      ack-mode: MANUAL
```

### Retry Configuration

```java
@Configuration
public class KafkaConfig {
    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
            .maxAttempts(3)
            .fixedBackoff(1000)
            .build();
    }
}
```

---

## Monitoring

### Kafka UI

Access Kafka UI at http://localhost:8090 to:

1. **View Topics** - See all topics and partitions
2. **Browse Messages** - View message content
3. **Consumer Groups** - Monitor consumer lag
4. **Cluster Health** - Check broker status

### Key Metrics

| Metric | Description |
|--------|-------------|
| Consumer Lag | Messages waiting to be processed |
| Message Rate | Messages per second |
| Partition Distribution | Load across partitions |
| Consumer Group Status | Active/inactive consumers |

---

## Best Practices

1. **Idempotency** - Use eventId to prevent duplicate processing
2. **Ordering** - Use orderId as partition key for ordered processing
3. **Schema Evolution** - Use backward-compatible changes
4. **Error Handling** - Implement DLQ for failed messages
5. **Monitoring** - Track consumer lag and message rates
