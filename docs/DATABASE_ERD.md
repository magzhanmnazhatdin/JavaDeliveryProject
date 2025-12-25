# Database Entity-Relationship Diagrams

## Overview

The system uses 4 separate PostgreSQL databases, one per microservice, following the database-per-service pattern.

| Service | Database | Port | Tables |
|---------|----------|------|--------|
| Order Service | order_db | 5433 | orders, order_items, payments |
| User Service | user_db | 5435 | users, addresses, user_preferences, favorite_restaurants |
| Restaurant Service | restaurant_db | 5434 | restaurants, menu_items, restaurant_orders, restaurant_order_items |
| Delivery Service | delivery_db | 5436 | couriers, deliveries |

---

## Order Service ERD

```mermaid
erDiagram
    orders ||--o{ order_items : contains
    orders ||--o| payments : has

    orders {
        bigint id PK
        varchar customer_id
        varchar restaurant_id
        varchar status
        decimal total_price
        varchar delivery_address
        varchar special_instructions
        timestamp created_at
        timestamp updated_at
    }

    order_items {
        bigint id PK
        bigint order_id FK
        varchar menu_item_id
        varchar name
        int quantity
        decimal price
        varchar special_instructions
    }

    payments {
        bigint id PK
        bigint order_id FK
        decimal amount
        varchar status
        varchar payment_method
        varchar transaction_id
        timestamp created_at
        timestamp updated_at
    }
```

### Order Status Flow

```mermaid
stateDiagram-v2
    [*] --> PENDING: Order Created
    PENDING --> CONFIRMED: Restaurant Accepts
    PENDING --> CANCELLED: Customer/System Cancels
    CONFIRMED --> PREPARING: Kitchen Starts
    PREPARING --> READY_FOR_PICKUP: Food Ready
    READY_FOR_PICKUP --> OUT_FOR_DELIVERY: Courier Picked Up
    OUT_FOR_DELIVERY --> DELIVERED: Delivery Complete
    DELIVERED --> [*]
    CANCELLED --> [*]
```

### Payment Status Flow

```mermaid
stateDiagram-v2
    [*] --> PENDING: Payment Initiated
    PENDING --> COMPLETED: Payment Success
    PENDING --> FAILED: Payment Failed
    COMPLETED --> REFUNDED: Admin Refunds
    FAILED --> [*]
    REFUNDED --> [*]
    COMPLETED --> [*]
```

---

## User Service ERD

```mermaid
erDiagram
    users ||--o{ addresses : has
    users ||--o| user_preferences : has
    users ||--o{ favorite_restaurants : has

    users {
        bigint id PK
        varchar keycloak_id UK
        varchar email UK
        varchar first_name
        varchar last_name
        varchar phone
        varchar role
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    addresses {
        bigint id PK
        bigint user_id FK
        varchar label
        varchar street
        varchar city
        varchar postal_code
        varchar country
        decimal latitude
        decimal longitude
        boolean is_default
        timestamp created_at
    }

    user_preferences {
        bigint id PK
        bigint user_id FK
        boolean email_notifications
        boolean push_notifications
        boolean sms_notifications
        varchar preferred_language
        varchar preferred_currency
        timestamp updated_at
    }

    favorite_restaurants {
        bigint id PK
        bigint user_id FK
        varchar restaurant_id
        timestamp added_at
    }
```

### User Status Flow

```mermaid
stateDiagram-v2
    [*] --> ACTIVE: User Registered
    ACTIVE --> SUSPENDED: Admin Suspends
    ACTIVE --> DELETED: User/Admin Deletes
    SUSPENDED --> ACTIVE: Admin Reactivates
    SUSPENDED --> DELETED: Admin Deletes
    DELETED --> [*]
```

### User Roles

```mermaid
mindmap
    root((User Roles))
        ADMIN
            Manage Users
            Manage Restaurants
            Manage Orders
            Manage Couriers
            Access All APIs
        CUSTOMER
            Create Orders
            Manage Profile
            View Restaurants
            Track Deliveries
        RESTAURANT
            Manage Menu
            Accept/Reject Orders
            Update Restaurant Info
        COURIER
            View Assigned Deliveries
            Update Location
            Update Delivery Status
```

---

## Restaurant Service ERD

```mermaid
erDiagram
    restaurants ||--o{ menu_items : has
    restaurants ||--o{ restaurant_orders : receives
    restaurant_orders ||--o{ restaurant_order_items : contains

    restaurants {
        bigint id PK
        varchar keycloak_id
        varchar name
        varchar description
        varchar address
        varchar city
        varchar phone
        varchar email
        varchar cuisine_type
        decimal latitude
        decimal longitude
        decimal average_rating
        int total_reviews
        varchar opening_hours
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }

    menu_items {
        bigint id PK
        bigint restaurant_id FK
        varchar name
        varchar description
        decimal price
        varchar category
        varchar image_url
        boolean is_available
        boolean is_vegetarian
        boolean is_vegan
        int preparation_time
        timestamp created_at
        timestamp updated_at
    }

    restaurant_orders {
        bigint id PK
        bigint restaurant_id FK
        varchar order_id
        varchar customer_id
        varchar status
        int estimated_prep_time
        varchar notes
        timestamp created_at
        timestamp updated_at
    }

    restaurant_order_items {
        bigint id PK
        bigint restaurant_order_id FK
        varchar menu_item_id
        varchar name_snapshot
        int quantity
        decimal price_snapshot
        varchar special_instructions
    }
```

### Restaurant Order Status Flow

```mermaid
stateDiagram-v2
    [*] --> RECEIVED: Order Event Received
    RECEIVED --> ACCEPTED: Restaurant Accepts
    RECEIVED --> REJECTED: Restaurant Rejects
    ACCEPTED --> PREPARING: Kitchen Starts
    PREPARING --> READY: Food Ready
    READY --> PICKED_UP: Courier Collects
    PICKED_UP --> [*]
    REJECTED --> [*]
```

---

## Delivery Service ERD

```mermaid
erDiagram
    couriers ||--o{ deliveries : delivers

    couriers {
        bigint id PK
        varchar keycloak_id
        varchar name
        varchar phone
        varchar email
        varchar vehicle_type
        varchar license_plate
        varchar status
        decimal current_latitude
        decimal current_longitude
        boolean is_available
        decimal average_rating
        int total_deliveries
        timestamp created_at
        timestamp updated_at
    }

    deliveries {
        bigint id PK
        varchar order_id UK
        bigint courier_id FK
        varchar customer_id
        varchar restaurant_id
        varchar status
        varchar pickup_address
        decimal pickup_latitude
        decimal pickup_longitude
        varchar delivery_address
        decimal delivery_latitude
        decimal delivery_longitude
        int estimated_time_minutes
        timestamp assigned_at
        timestamp picked_up_at
        timestamp delivered_at
        varchar notes
        timestamp created_at
        timestamp updated_at
    }
```

### Delivery Status Flow

```mermaid
stateDiagram-v2
    [*] --> PENDING: Delivery Created
    PENDING --> ASSIGNED: Courier Assigned
    ASSIGNED --> PICKED_UP: Courier at Restaurant
    PICKED_UP --> IN_TRANSIT: En Route
    IN_TRANSIT --> DELIVERED: Order Delivered
    PENDING --> CANCELLED: Order Cancelled
    ASSIGNED --> CANCELLED: Order Cancelled
    DELIVERED --> [*]
    CANCELLED --> [*]
```

### Courier Status Flow

```mermaid
stateDiagram-v2
    [*] --> AVAILABLE: Courier Online
    AVAILABLE --> ON_DELIVERY: Assigned to Order
    ON_DELIVERY --> AVAILABLE: Delivery Complete
    AVAILABLE --> OFFLINE: Courier Goes Offline
    ON_DELIVERY --> OFFLINE: Emergency/Break
    OFFLINE --> AVAILABLE: Courier Back Online
    OFFLINE --> [*]: Account Deactivated
```

---

## Cross-Service References

Since each service has its own database, references between services use IDs stored as VARCHAR:

```mermaid
flowchart LR
    subgraph OrderService["Order Service DB"]
        O_orders[orders]
        O_items[order_items]
    end

    subgraph UserService["User Service DB"]
        U_users[users]
    end

    subgraph RestaurantService["Restaurant Service DB"]
        R_restaurants[restaurants]
        R_menu[menu_items]
    end

    subgraph DeliveryService["Delivery Service DB"]
        D_deliveries[deliveries]
        D_couriers[couriers]
    end

    O_orders -->|customer_id| U_users
    O_orders -->|restaurant_id| R_restaurants
    O_items -->|menu_item_id| R_menu
    D_deliveries -->|order_id| O_orders
    D_deliveries -->|customer_id| U_users
    D_deliveries -->|restaurant_id| R_restaurants
```

## Flyway Migrations

### Order Service
| Version | Description |
|---------|-------------|
| V1 | Create orders table |
| V2 | Create order_items table |
| V3 | Create payments table |

### User Service
| Version | Description |
|---------|-------------|
| V1 | Create users table |
| V2 | Create addresses table |
| V3 | Create user_preferences table |
| V4 | Create favorite_restaurants table |

### Restaurant Service
| Version | Description |
|---------|-------------|
| V1 | Create restaurants table |
| V2 | Create menu_items table |
| V3 | Create restaurant_orders table |
| V4 | Create restaurant_order_items table |

### Delivery Service
| Version | Description |
|---------|-------------|
| V1 | Create couriers table |
| V2 | Create deliveries table |
