# 🚀 Food Delivery Microservices Platform

A microservice-based food delivery system built with Spring Boot, demonstrating modern enterprise architecture patterns.

## 📋 Table of Contents

- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Microservices](#-microservices)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [Keycloak Authentication](#-keycloak-authentication)
- [Kafka Events](#-kafka-events)
- [Database Schema](#-database-schema)
- [Testing](#-testing)

---

## 🛠 Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 17** | Programming language |
| **Spring Boot 3.x** | Application framework |
| **Spring Cloud Gateway** | API Gateway with routing, rate limiting, circuit breaker |
| **Spring Security + OAuth2** | Security with Keycloak integration |
| **Spring Data JPA** | Database access |
| **PostgreSQL 15** | Relational database |
| **Flyway** | Database migrations |
| **Apache Kafka** | Event-driven messaging |
| **Keycloak 23** | Identity and Access Management |
| **Redis** | Caching and rate limiting |
| **Docker & Docker Compose** | Containerization |
| **Swagger/OpenAPI 3** | API documentation |
| **JUnit 5 + Mockito** | Testing |
| **Resilience4j** | Circuit breaker pattern |

---

## 🏗 Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
│                    (Web App, Mobile App, Postman)                           │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           KEYCLOAK (8180)                                    │
│                    Identity Provider / OAuth2 Server                         │
│                         Realm: delivery-realm                                │
└─────────────────────────────────┬───────────────────────────────────────────┘
                                  │ JWT Token
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         API GATEWAY (8080)                                   │
│              Spring Cloud Gateway + Rate Limiter + Circuit Breaker          │
└───────┬─────────────┬─────────────┬─────────────┬───────────────────────────┘
        │             │             │             │
        ▼             ▼             ▼             ▼
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│  ORDER    │  │   USER    │  │RESTAURANT │  │ DELIVERY  │
│  SERVICE  │  │  SERVICE  │  │  SERVICE  │  │  SERVICE  │
│  (8081)   │  │  (8083)   │  │  (8082)   │  │  (8084)   │
└─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
      │              │              │              │
      ▼              ▼              ▼              ▼
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│ PostgreSQL│  │ PostgreSQL│  │ PostgreSQL│  │ PostgreSQL│
│   :5433   │  │   :5435   │  │   :5434   │  │   :5436   │
└───────────┘  └───────────┘  └───────────┘  └───────────┘

                    ┌─────────────────────────┐
                    │      KAFKA (9092)       │
                    │    Event Bus / Topics   │
                    │  ┌───────────────────┐  │
                    │  │ order-events      │  │
                    │  │ payment-events    │  │
                    │  │ user-events       │  │
                    │  │ delivery-events   │  │
                    │  │ restaurant-events │  │
                    │  └───────────────────┘  │
                    └────────────┬────────────┘
                                 │
                    ┌────────────┴────────────┐
                    │    ZOOKEEPER (2181)     │
                    └─────────────────────────┘
```

### Kafka Event Flow

```
┌──────────────┐     ORDER_CREATED      ┌──────────────────┐
│    ORDER     │ ───────────────────▶   │    RESTAURANT    │
│   SERVICE    │                        │     SERVICE      │
└──────────────┘                        └────────┬─────────┘
                                                 │
                                    ORDER_ACCEPTED/ORDER_READY
                                                 │
                                                 ▼
                                        ┌──────────────────┐
                                        │    DELIVERY      │
                                        │     SERVICE      │
                                        └──────────────────┘
```

### Keycloak Authentication Flow

```
┌────────┐      1. Login Request       ┌──────────────┐
│ Client │ ──────────────────────────▶ │   Keycloak   │
└────────┘                             └──────┬───────┘
     ▲                                        │
     │         2. JWT Access Token            │
     └────────────────────────────────────────┘
     │
     │         3. API Request + Bearer Token
     ▼
┌─────────────────┐    4. Validate Token    ┌──────────────┐
│   API Gateway   │ ◀─────────────────────▶ │   Keycloak   │
└────────┬────────┘                         └──────────────┘
         │
         │  5. Forward Request (if valid)
         ▼
┌─────────────────┐
│  Microservice   │
└─────────────────┘
```

---

## 📦 Microservices

### 1. API Gateway (Port: 8080)
- Request routing to microservices
- Rate limiting via Redis
- Circuit breaker (Resilience4j)
- JWT token validation
- CORS configuration

### 2. Order Service (Port: 8081)
- Order management (create, update, cancel, delete)
- Payment processing
- Kafka producer for order events

**Order Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | `/api/orders` | Create order | CUSTOMER, ADMIN |
| GET | `/api/orders/{id}` | Get order by ID | Authenticated |
| GET | `/api/orders/my-orders` | Get current user's orders | CUSTOMER, ADMIN |
| GET | `/api/orders/customer/{customerId}` | Get orders by customer | ADMIN |
| GET | `/api/orders/restaurant/{restaurantId}` | Get orders by restaurant | RESTAURANT, ADMIN |
| GET | `/api/orders/restaurant/{restaurantId}/active` | Get active orders | RESTAURANT, ADMIN |
| GET | `/api/orders/status/{status}` | Get orders by status | ADMIN |
| PUT | `/api/orders/{id}` | Update order details | CUSTOMER, ADMIN |
| PATCH | `/api/orders/{id}/status` | Update order status | RESTAURANT, COURIER, ADMIN |
| POST | `/api/orders/{id}/cancel` | Cancel order | CUSTOMER, ADMIN |
| DELETE | `/api/orders/{id}` | Delete order (pending/cancelled only) | ADMIN |

**Payment Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | `/api/payments/process` | Process payment | CUSTOMER, ADMIN |
| GET | `/api/payments/{id}` | Get payment by ID | CUSTOMER, ADMIN |
| GET | `/api/payments/order/{orderId}` | Get payment by order | CUSTOMER, RESTAURANT, ADMIN |
| GET | `/api/payments/my-payments` | Get current user's payments | CUSTOMER, ADMIN |
| PUT | `/api/payments/{id}` | Update payment method (pending only) | CUSTOMER, ADMIN |
| POST | `/api/payments/{id}/refund` | Refund payment | ADMIN |
| POST | `/api/payments/{id}/cancel` | Cancel payment | ADMIN |

### 3. User Service (Port: 8083)
- User profile management
- Address management
- User preferences
- Favorite restaurants
- Kafka producer for user events

**User Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/users/me` | Get current user profile | Authenticated |
| PUT | `/api/users/me` | Update profile | Authenticated |
| GET | `/api/users/{id}` | Get user by ID | ADMIN |
| GET | `/api/users` | Get all users | ADMIN |
| DELETE | `/api/users/{id}` | Delete user | ADMIN |

**Address Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | `/api/addresses` | Add new address | Authenticated |
| GET | `/api/addresses` | Get my addresses | Authenticated |
| GET | `/api/addresses/{id}` | Get address by ID | Authenticated |
| PUT | `/api/addresses/{id}` | Update address | Authenticated |
| DELETE | `/api/addresses/{id}` | Delete address | Authenticated |
| POST | `/api/addresses/{id}/set-default` | Set as default address | Authenticated |
| PUT | `/api/addresses/user/{userId}/{addressId}` | Update user's address | ADMIN |
| DELETE | `/api/addresses/user/{userId}/{addressId}` | Delete user's address | ADMIN |
| POST | `/api/addresses/user/{userId}/{addressId}/set-default` | Set user's default | ADMIN |

**Preferences Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/preferences` | Get my preferences | Authenticated |
| PUT | `/api/preferences` | Update preferences | Authenticated |
| DELETE | `/api/preferences` | Reset preferences to defaults | Authenticated |
| GET | `/api/preferences/user/{userId}` | Get user's preferences | ADMIN |
| PUT | `/api/preferences/user/{userId}` | Update user's preferences | ADMIN |
| DELETE | `/api/preferences/user/{userId}` | Reset user's preferences | ADMIN |

**Favorites Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/favorites/restaurants` | Get favorite restaurants | Authenticated |
| POST | `/api/favorites/restaurants/{id}` | Add favorite restaurant | Authenticated |
| DELETE | `/api/favorites/restaurants/{id}` | Remove favorite restaurant | Authenticated |

### 4. Restaurant Service (Port: 8082)
- Restaurant management
- Menu item management
- Restaurant order processing
- Kafka consumer for order events
- Kafka producer for restaurant events

**Restaurant Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/restaurants` | List all restaurants | Public |
| GET | `/api/restaurants/{id}` | Get restaurant by ID | Public |
| GET | `/api/restaurants/search` | Search restaurants | Public |
| GET | `/api/restaurants/nearby` | Find nearby restaurants | Public |
| POST | `/api/restaurants` | Create restaurant | RESTAURANT, ADMIN |
| PUT | `/api/restaurants/{id}` | Update restaurant | RESTAURANT, ADMIN |
| DELETE | `/api/restaurants/{id}` | Delete restaurant | ADMIN |

**Menu Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/restaurants/{id}/menu` | Get restaurant menu | Public |
| GET | `/api/menu-items/{id}` | Get menu item by ID | Public |
| POST | `/api/restaurants/{id}/menu` | Add menu item | RESTAURANT, ADMIN |
| PUT | `/api/menu-items/{id}` | Update menu item | RESTAURANT, ADMIN |
| DELETE | `/api/menu-items/{id}` | Delete menu item | RESTAURANT, ADMIN |

**Restaurant Order Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| GET | `/api/restaurant-orders/{id}` | Get order by ID | RESTAURANT, ADMIN |
| GET | `/api/restaurant-orders/by-order/{orderId}` | Get by original order ID | RESTAURANT, ADMIN |
| GET | `/api/restaurant-orders/restaurant/{restaurantId}` | Get orders by restaurant | RESTAURANT, ADMIN |
| GET | `/api/restaurant-orders/restaurant/{restaurantId}/pending` | Get pending orders | RESTAURANT, ADMIN |
| GET | `/api/restaurant-orders/restaurant/{restaurantId}/active` | Get active orders | RESTAURANT, ADMIN |
| POST | `/api/restaurant-orders/{id}/accept` | Accept order | RESTAURANT, ADMIN |
| POST | `/api/restaurant-orders/{id}/reject` | Reject order | RESTAURANT, ADMIN |
| POST | `/api/restaurant-orders/{id}/start-preparing` | Start preparing | RESTAURANT, ADMIN |
| POST | `/api/restaurant-orders/{id}/ready` | Mark as ready | RESTAURANT, ADMIN |
| POST | `/api/restaurant-orders/{id}/picked-up` | Mark as picked up | RESTAURANT, ADMIN |
| DELETE | `/api/restaurant-orders/{id}` | Delete order (pending/rejected/cancelled only) | ADMIN |

### 5. Delivery Service (Port: 8084)
- Delivery management
- Courier management
- Real-time location updates
- Kafka consumer for order events
- Kafka producer for delivery events

**Delivery Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | `/api/deliveries` | Create delivery | ADMIN, RESTAURANT |
| GET | `/api/deliveries/{id}` | Get delivery by ID | Authenticated |
| GET | `/api/deliveries/order/{orderId}` | Get delivery by order ID | Authenticated |
| GET | `/api/deliveries/courier/{courierId}` | Get deliveries by courier | COURIER, ADMIN |
| GET | `/api/deliveries/customer/{customerId}` | Get deliveries by customer | CUSTOMER, ADMIN |
| GET | `/api/deliveries/status/{status}` | Get deliveries by status | ADMIN |
| GET | `/api/deliveries` | Get all deliveries | ADMIN |
| PUT | `/api/deliveries/{id}` | Update delivery details | ADMIN, RESTAURANT |
| POST | `/api/deliveries/{id}/assign` | Assign courier manually | ADMIN |
| POST | `/api/deliveries/{id}/assign-auto` | Auto-assign courier | ADMIN |
| PUT | `/api/deliveries/{id}/status` | Update delivery status | COURIER, ADMIN |
| DELETE | `/api/deliveries/{id}` | Delete delivery (pending/cancelled only) | ADMIN |

**Courier Endpoints:**
| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | `/api/couriers` | Register courier | ADMIN |
| GET | `/api/couriers/{id}` | Get courier by ID | COURIER, ADMIN |
| GET | `/api/couriers/keycloak/{keycloakId}` | Get courier by Keycloak ID | COURIER, ADMIN |
| GET | `/api/couriers` | Get all couriers | ADMIN |
| GET | `/api/couriers/available` | Get available couriers | ADMIN |
| PUT | `/api/couriers/{id}` | Update courier | COURIER, ADMIN |
| PUT | `/api/couriers/{id}/status` | Update courier status | COURIER, ADMIN |
| PUT | `/api/couriers/{id}/location` | Update courier location | COURIER, ADMIN |
| DELETE | `/api/couriers/{id}` | Delete courier | ADMIN |

---

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17+ (for local development)
- Maven 3.9+

### Option 1: Run with Docker Compose (Recommended)

```bash
# Clone the repository
git clone <repository-url>
cd JavaDeliveryProject

# Start infrastructure (databases, kafka, keycloak, redis)
docker compose up -d

# Wait for services to be healthy (~60 seconds)
docker compose ps

# Start microservices
docker compose -f docker-compose.services.yml up -d

# Or run everything together
docker compose -f docker-compose.full.yml up -d
```

### Option 2: Run Locally

1. **Start infrastructure:**
```bash
docker compose up -d
```

2. **Run each service:**
```bash
# Terminal 1 - Order Service
cd order-service
./mvnw spring-boot:run

# Terminal 2 - User Service
cd user-service
./mvnw spring-boot:run

# Terminal 3 - Restaurant Service
cd restaurant-service
./mvnw spring-boot:run

# Terminal 4 - Delivery Service
cd delivery-service
./mvnw spring-boot:run

# Terminal 5 - API Gateway
cd api-gateway
./mvnw spring-boot:run
```

### Verify Services

| Service | URL | Health Check |
|---------|-----|--------------|
| API Gateway | http://localhost:8080 | http://localhost:8080/actuator/health |
| Order Service | http://localhost:8081 | http://localhost:8081/actuator/health |
| Restaurant Service | http://localhost:8082 | http://localhost:8082/actuator/health |
| User Service | http://localhost:8083 | http://localhost:8083/actuator/health |
| Delivery Service | http://localhost:8084 | http://localhost:8084/actuator/health |
| Keycloak | http://localhost:8180 | - |
| Kafka UI | http://localhost:8090 | - |

---

## 📚 API Documentation

Swagger UI is available for each service:

| Service | Swagger UI |
|---------|------------|
| Order Service | http://localhost:8081/swagger-ui.html |
| Restaurant Service | http://localhost:8082/swagger-ui.html |
| User Service | http://localhost:8083/swagger-ui.html |
| Delivery Service | http://localhost:8084/swagger-ui.html |

### OpenAPI JSON

- Order Service: http://localhost:8081/v3/api-docs
- Restaurant Service: http://localhost:8082/v3/api-docs
- User Service: http://localhost:8083/v3/api-docs
- Delivery Service: http://localhost:8084/v3/api-docs

---

## 🔐 Keycloak Authentication

### Keycloak Setup

1. **Access Keycloak Admin Console:**
   - URL: http://localhost:8180
   - Username: `admin`
   - Password: `admin`

2. **Realm:** `delivery-realm`

3. **Roles:**
   - `ADMIN` - System administrator
   - `CUSTOMER` - Regular customer
   - `RESTAURANT` - Restaurant owner
   - `COURIER` - Delivery courier

4. **Client:** `delivery-api` (configured for backend API)

### Getting Access Token

#### Option A: Using Postman

1. Create a new request in Postman
2. Go to **Authorization** tab
3. Select **OAuth 2.0**
4. Configure:
   - Grant Type: `Password Credentials`
   - Access Token URL: `http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token`
   - Client ID: `delivery-api`
   - Client Secret: `<your-client-secret>`
   - Username: `<keycloak-user>`
   - Password: `<keycloak-password>`
5. Click **Get New Access Token**

#### Option B: Using cURL

```bash
# Get access token
curl -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "client_secret=<your-client-secret>" \
  -d "username=<username>" \
  -d "password=<password>"

# Use token in API request
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <access_token>"
```

### Role-Based Access Control

| Endpoint Pattern | Required Role |
|-----------------|---------------|
| `/swagger-ui/**`, `/v3/api-docs/**` | Public |
| `/actuator/health` | Public |
| `GET /api/restaurants/**` | Public |
| `/api/users/me`, `/api/orders/my-orders` | Authenticated |
| `/api/admin/**` | ADMIN |
| `POST /api/restaurants` | RESTAURANT, ADMIN |
| `PATCH /api/couriers/{id}/location` | COURIER, ADMIN |

---

## 📨 Kafka Events

### Topics

| Topic | Producer | Consumer | Events |
|-------|----------|----------|--------|
| `order-events` | Order Service | Restaurant, Delivery | ORDER_CREATED, ORDER_CANCELLED |
| `payment-events` | Order Service | - | PAYMENT_COMPLETED, PAYMENT_FAILED |
| `user-events` | User Service | - | USER_CREATED, USER_UPDATED |
| `restaurant-events` | Restaurant Service | Delivery | ORDER_ACCEPTED, ORDER_REJECTED, ORDER_READY |
| `delivery-events` | Delivery Service | - | COURIER_ASSIGNED, DELIVERY_STATUS_CHANGED |

### Event Flow Example

1. **Customer creates order** → Order Service publishes `ORDER_CREATED`
2. **Restaurant receives event** → Creates RestaurantOrder, starts preparation
3. **Restaurant accepts** → Publishes `ORDER_ACCEPTED`
4. **Delivery Service receives** → Creates Delivery record
5. **Restaurant ready** → Publishes `ORDER_READY`
6. **Courier assigned** → Delivery Service publishes `COURIER_ASSIGNED`
7. **Delivery completed** → Publishes `DELIVERY_STATUS_CHANGED`

### Kafka UI

Access Kafka UI at http://localhost:8090 to:
- View topics and messages
- Monitor consumer groups
- Check cluster health

---

## 🗄 Database Schema

### Order Service (PostgreSQL: 5433)

```
┌─────────────────────┐       ┌─────────────────────┐
│       orders        │       │    order_items      │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │──────<│ id (PK)             │
│ customer_id         │       │ order_id (FK)       │
│ restaurant_id       │       │ menu_item_id        │
│ status              │       │ quantity            │
│ total_price         │       │ price               │
│ delivery_address    │       │ name                │
│ created_at          │       └─────────────────────┘
│ updated_at          │
└──────────┬──────────┘
           │
           │       ┌─────────────────────┐
           └──────>│     payments        │
                   ├─────────────────────┤
                   │ id (PK)             │
                   │ order_id (FK)       │
                   │ amount              │
                   │ status              │
                   │ payment_method      │
                   │ transaction_id      │
                   └─────────────────────┘
```

### User Service (PostgreSQL: 5435)

```
┌─────────────────────┐       ┌─────────────────────┐
│       users         │       │     addresses       │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │──────<│ id (PK)             │
│ keycloak_id         │       │ user_id (FK)        │
│ email               │       │ street              │
│ first_name          │       │ city                │
│ last_name           │       │ latitude            │
│ phone               │       │ longitude           │
│ role                │       │ is_default          │
│ status              │       └─────────────────────┘
└──────────┬──────────┘
           │
           │       ┌─────────────────────┐
           ├──────>│  user_preferences   │
           │       ├─────────────────────┤
           │       │ id (PK)             │
           │       │ user_id (FK)        │
           │       │ email_notifications │
           │       │ push_notifications  │
           │       │ sms_notifications   │
           │       └─────────────────────┘
           │
           │       ┌─────────────────────┐
           └──────>│favorite_restaurants │
                   ├─────────────────────┤
                   │ id (PK)             │
                   │ user_id (FK)        │
                   │ restaurant_id       │
                   └─────────────────────┘
```

### Restaurant Service (PostgreSQL: 5434)

```
┌─────────────────────┐       ┌─────────────────────┐
│    restaurants      │       │    menu_items       │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │──────<│ id (PK)             │
│ keycloak_id         │       │ restaurant_id (FK)  │
│ name                │       │ name                │
│ description         │       │ description         │
│ address             │       │ price               │
│ city                │       │ category            │
│ phone               │       │ is_available        │
│ cuisine_type        │       └─────────────────────┘
│ latitude            │
│ longitude           │       ┌─────────────────────┐
│ is_active           │──────<│ restaurant_orders   │
└─────────────────────┘       ├─────────────────────┤
                              │ id (PK)             │
                              │ restaurant_id (FK)  │
                              │ order_id            │
                              │ status              │
                              │ estimated_prep_time │
                              └─────────────────────┘
```

### Delivery Service (PostgreSQL: 5436)

```
┌─────────────────────┐       ┌─────────────────────┐
│      couriers       │       │     deliveries      │
├─────────────────────┤       ├─────────────────────┤
│ id (PK)             │──────<│ id (PK)             │
│ keycloak_id         │       │ order_id            │
│ name                │       │ courier_id (FK)     │
│ phone               │       │ status              │
│ vehicle_type        │       │ pickup_address      │
│ status              │       │ delivery_address    │
│ current_latitude    │       │ pickup_latitude     │
│ current_longitude   │       │ pickup_longitude    │
│ is_available        │       │ delivery_latitude   │
└─────────────────────┘       │ delivery_longitude  │
                              │ estimated_time      │
                              │ actual_delivery_time│
                              └─────────────────────┘
```

---

## 🧪 Testing

### Run All Tests

```bash
# Run tests for all services
./mvnw test

# Run tests for specific service
cd order-service
./mvnw test
```

### Test Types

| Type | Description | Example |
|------|-------------|---------|
| **Unit Tests** | Service layer tests with Mockito | `OrderServiceTest` |
| **Integration Tests** | Controller tests with MockMvc | `OrderControllerIntegrationTest` |
| **Kafka Tests** | Embedded Kafka tests | `UserEventProducerIntegrationTest` |

### Test Coverage

| Service | Unit Tests | Integration Tests | Kafka Tests |
|---------|------------|-------------------|-------------|
| Order Service | ✅ | ✅ | ✅ |
| User Service | ✅ | ✅ | ✅ |
| Restaurant Service | ✅ | ✅ | - |
| Delivery Service | ✅ | ✅ | - |

---

## 📁 Project Structure

```
JavaDeliveryProject/
├── api-gateway/                 # Spring Cloud Gateway
│   ├── src/main/java/.../
│   │   ├── config/             # Security, CORS, Gateway config
│   │   └── controller/         # Fallback controller
│   └── Dockerfile
│
├── order-service/               # Order & Payment management
│   ├── src/main/java/.../
│   │   ├── controller/         # REST endpoints
│   │   ├── service/            # Business logic
│   │   ├── repository/         # Data access
│   │   ├── entity/             # JPA entities
│   │   ├── dto/                # Data transfer objects
│   │   ├── kafka/              # Kafka producers
│   │   ├── config/             # Security, OpenAPI
│   │   └── exception/          # Exception handlers
│   ├── src/main/resources/
│   │   ├── db/migration/       # Flyway migrations
│   │   └── application.yml
│   └── Dockerfile
│
├── user-service/                # User management
├── restaurant-service/          # Restaurant & Menu management
├── delivery-service/            # Delivery & Courier management
│
├── docker-compose.yml           # Infrastructure
├── docker-compose.services.yml  # Microservices
├── docker-compose.full.yml      # Full stack
└── README.md
```

---

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |
| `DATABASE_URL` | PostgreSQL URL | varies by service |
| `DATABASE_USERNAME` | DB username | `postgres` |
| `DATABASE_PASSWORD` | DB password | `postgres` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | `localhost:9092` |
| `KEYCLOAK_ISSUER_URI` | Keycloak issuer | `http://localhost:8180/realms/delivery-realm` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |

### Profiles

- `default` - Local development
- `docker` - Docker environment
- `test` - Testing environment

---

## 📞 Support

For issues and questions, please create an issue in the repository.

---

## 📄 License

This project is created for educational purposes as part of a Spring Boot microservices course.
