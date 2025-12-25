# Architecture Documentation

## High-Level Architecture Diagram

```mermaid
flowchart TB
    subgraph Clients
        WEB[Web Application]
        MOBILE[Mobile App]
        POSTMAN[Postman/API Client]
    end

    subgraph Security["Identity Provider"]
        KC[Keycloak<br/>:8180]
    end

    subgraph Gateway["API Gateway Layer"]
        GW[API Gateway<br/>:8080<br/>Spring Cloud Gateway]
        RL[Rate Limiter]
        CB[Circuit Breaker]
    end

    subgraph Services["Microservices Layer"]
        OS[Order Service<br/>:8081]
        US[User Service<br/>:8083]
        RS[Restaurant Service<br/>:8082]
        DS[Delivery Service<br/>:8084]
    end

    subgraph Databases["Database Layer"]
        PG_O[(PostgreSQL<br/>Order DB<br/>:5433)]
        PG_U[(PostgreSQL<br/>User DB<br/>:5435)]
        PG_R[(PostgreSQL<br/>Restaurant DB<br/>:5434)]
        PG_D[(PostgreSQL<br/>Delivery DB<br/>:5436)]
    end

    subgraph Messaging["Message Broker"]
        KAFKA[Apache Kafka<br/>:9092]
        ZK[Zookeeper<br/>:2181]
    end

    subgraph Cache["Caching Layer"]
        REDIS[(Redis<br/>:6379)]
    end

    WEB --> KC
    MOBILE --> KC
    POSTMAN --> KC

    KC -->|JWT Token| GW
    WEB -->|Bearer Token| GW
    MOBILE -->|Bearer Token| GW
    POSTMAN -->|Bearer Token| GW

    GW --> RL
    GW --> CB
    RL --> REDIS

    GW --> OS
    GW --> US
    GW --> RS
    GW --> DS

    OS --> PG_O
    US --> PG_U
    RS --> PG_R
    DS --> PG_D

    OS -->|Produce| KAFKA
    US -->|Produce| KAFKA
    RS -->|Produce/Consume| KAFKA
    DS -->|Produce/Consume| KAFKA

    KAFKA --> ZK
```

## Service Communication

### Synchronous Communication (REST)

```mermaid
sequenceDiagram
    participant C as Client
    participant KC as Keycloak
    participant GW as API Gateway
    participant S as Microservice
    participant DB as PostgreSQL

    C->>KC: 1. Login (username/password)
    KC-->>C: 2. JWT Access Token
    C->>GW: 3. API Request + Bearer Token
    GW->>KC: 4. Validate Token
    KC-->>GW: 5. Token Valid
    GW->>S: 6. Forward Request
    S->>DB: 7. Database Query
    DB-->>S: 8. Data
    S-->>GW: 9. Response
    GW-->>C: 10. Response
```

### Asynchronous Communication (Kafka)

```mermaid
sequenceDiagram
    participant OS as Order Service
    participant K as Kafka
    participant RS as Restaurant Service
    participant DS as Delivery Service

    OS->>K: Publish ORDER_CREATED
    K-->>RS: Consume ORDER_CREATED
    RS->>RS: Create RestaurantOrder
    RS->>K: Publish ORDER_ACCEPTED
    K-->>DS: Consume ORDER_ACCEPTED
    DS->>DS: Create Delivery
    RS->>K: Publish ORDER_READY
    K-->>DS: Consume ORDER_READY
    DS->>DS: Assign Courier
    DS->>K: Publish COURIER_ASSIGNED
```

## API Gateway Configuration

```mermaid
flowchart LR
    subgraph Gateway["API Gateway :8080"]
        direction TB
        ROUTE[Route Handler]
        SEC[Security Filter]
        RL[Rate Limiter]
        CB[Circuit Breaker]
        LB[Load Balancer]
    end

    subgraph Routes
        R1["/api/orders/**"]
        R2["/api/users/**"]
        R3["/api/restaurants/**"]
        R4["/api/deliveries/**"]
    end

    subgraph Services
        OS[Order :8081]
        US[User :8083]
        RS[Restaurant :8082]
        DS[Delivery :8084]
    end

    REQUEST[Request] --> SEC
    SEC --> RL
    RL --> ROUTE
    ROUTE --> CB
    CB --> LB

    LB --> R1 --> OS
    LB --> R2 --> US
    LB --> R3 --> RS
    LB --> R4 --> DS
```

## Component Diagram

```mermaid
flowchart TB
    subgraph OrderService["Order Service"]
        OC[OrderController]
        PC[PaymentController]
        OSvc[OrderService]
        PSvc[PaymentService]
        ORepo[OrderRepository]
        PRepo[PaymentRepository]
        OKafka[OrderEventProducer]
    end

    subgraph UserService["User Service"]
        UC[UserController]
        AC[AddressController]
        USvc[UserService]
        ASvc[AddressService]
        URepo[UserRepository]
        ARepo[AddressRepository]
        UKafka[UserEventProducer]
    end

    subgraph RestaurantService["Restaurant Service"]
        RC[RestaurantController]
        MC[MenuItemController]
        RSvc[RestaurantService]
        MSvc[MenuItemService]
        RRepo[RestaurantRepository]
        MRepo[MenuItemRepository]
        RKafka[RestaurantEventProducer]
        OListener[OrderEventsListener]
    end

    subgraph DeliveryService["Delivery Service"]
        DC[DeliveryController]
        CC[CourierController]
        DSvc[DeliveryService]
        CSvc[CourierService]
        DRepo[DeliveryRepository]
        CRepo[CourierRepository]
        DKafka[DeliveryEventProducer]
        DListener[OrderEventsListener]
    end

    OC --> OSvc --> ORepo
    PC --> PSvc --> PRepo
    OSvc --> OKafka

    UC --> USvc --> URepo
    AC --> ASvc --> ARepo
    USvc --> UKafka

    RC --> RSvc --> RRepo
    MC --> MSvc --> MRepo
    RSvc --> RKafka
    OListener --> RSvc

    DC --> DSvc --> DRepo
    CC --> CSvc --> CRepo
    DSvc --> DKafka
    DListener --> DSvc
```

## Deployment Diagram

```mermaid
flowchart TB
    subgraph DockerNetwork["Docker Network: delivery-network"]
        subgraph Infrastructure
            ZK[Zookeeper]
            KAFKA[Kafka]
            REDIS[Redis]
            KC[Keycloak]
            KAFKA_UI[Kafka UI]
        end

        subgraph Databases
            PG_O[(postgres-order)]
            PG_U[(postgres-user)]
            PG_R[(postgres-restaurant)]
            PG_D[(postgres-delivery)]
            PG_K[(postgres-keycloak)]
        end

        subgraph Applications
            GW[api-gateway:8080]
            OS[order-service:8081]
            RS[restaurant-service:8082]
            US[user-service:8083]
            DS[delivery-service:8084]
        end
    end

    KAFKA --> ZK
    KC --> PG_K

    OS --> PG_O
    OS --> KAFKA

    US --> PG_U
    US --> KAFKA

    RS --> PG_R
    RS --> KAFKA

    DS --> PG_D
    DS --> KAFKA

    GW --> REDIS
    GW --> OS
    GW --> US
    GW --> RS
    GW --> DS
```

## Technology Stack

| Layer | Technology | Purpose |
|-------|------------|---------|
| Gateway | Spring Cloud Gateway | Routing, Rate Limiting, Circuit Breaking |
| Security | Keycloak + Spring Security OAuth2 | Authentication & Authorization |
| Services | Spring Boot 3.x | Microservices Framework |
| Database | PostgreSQL 15 + Spring Data JPA | Data Persistence |
| Migrations | Flyway | Database Schema Management |
| Messaging | Apache Kafka | Event-Driven Communication |
| Caching | Redis | Rate Limiting, Session Cache |
| Documentation | OpenAPI 3.0 / Swagger | API Documentation |
| Containerization | Docker & Docker Compose | Deployment |
| Resilience | Resilience4j | Circuit Breaker, Retry |
