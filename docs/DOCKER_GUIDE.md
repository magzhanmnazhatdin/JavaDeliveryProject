# Docker Guide

Complete guide for running the Food Delivery Platform using Docker and Docker Compose.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Docker Compose Files](#docker-compose-files)
- [Quick Start](#quick-start)
- [Infrastructure Services](#infrastructure-services)
- [Microservices](#microservices)
- [Environment Variables](#environment-variables)
- [Volumes & Persistence](#volumes--persistence)
- [Networking](#networking)
- [Building Images](#building-images)
- [Useful Commands](#useful-commands)
- [Production Considerations](#production-considerations)

---

## Prerequisites

### Required Software

| Software | Minimum Version | Recommended |
|----------|----------------|-------------|
| Docker | 20.10+ | Latest |
| Docker Compose | 2.0+ | Latest |
| RAM | 8 GB | 16 GB |
| Disk Space | 10 GB | 20 GB |

### Verify Installation

```bash
docker --version
docker compose version
```

---

## Docker Compose Files

The project includes three Docker Compose configurations:

| File | Purpose | Usage |
|------|---------|-------|
| `docker-compose.yml` | Infrastructure only | Development with local services |
| `docker-compose.services.yml` | Microservices only | Requires infrastructure running |
| `docker-compose.full.yml` | Complete stack | All-in-one deployment |

### File Descriptions

#### docker-compose.yml (Infrastructure)

```
Services included:
├── postgres-order (Port 5433)
├── postgres-restaurant (Port 5434)
├── postgres-user (Port 5435)
├── postgres-delivery (Port 5436)
├── postgres-keycloak (internal)
├── zookeeper (Port 2181)
├── kafka (Ports 9092, 29092)
├── kafka-ui (Port 8090)
├── redis (Port 6379)
└── keycloak (Port 8180)
```

#### docker-compose.services.yml (Microservices)

```
Services included:
├── api-gateway (Port 8080)
├── order-service (Port 8081)
├── restaurant-service (Port 8082)
├── user-service (Port 8083)
└── delivery-service (Port 8084)
```

#### docker-compose.full.yml (Complete Stack)

Combines all infrastructure and microservices in a single file.

---

## Quick Start

### Option 1: Infrastructure + Local Development

Best for development when you want to run services in IDE.

```bash
# Start infrastructure
docker compose up -d

# Wait for services to be healthy (~60 seconds)
docker compose ps

# Run services locally via IDE or:
cd order-service && ./mvnw spring-boot:run
```

### Option 2: Full Docker Deployment

Best for testing the complete system.

```bash
# Build and start everything
docker compose -f docker-compose.full.yml up -d --build

# Watch logs
docker compose -f docker-compose.full.yml logs -f
```

### Option 3: Infrastructure + Docker Services

```bash
# Step 1: Start infrastructure
docker compose up -d

# Step 2: Wait for infrastructure
docker compose ps  # All should be healthy

# Step 3: Start microservices
docker compose -f docker-compose.services.yml up -d --build
```

---

## Infrastructure Services

### PostgreSQL Databases

Four isolated PostgreSQL instances for microservice data isolation.

| Service | Container | Port | Database | User |
|---------|-----------|------|----------|------|
| Order | postgres-order | 5433 | order_db | order_user |
| Restaurant | postgres-restaurant | 5434 | restaurant_db | restaurant_user |
| User | postgres-user | 5435 | user_db | user_user |
| Delivery | postgres-delivery | 5436 | delivery_db | delivery_user |
| Keycloak | postgres-keycloak | - | keycloak | keycloak |

**Connect to database:**

```bash
# Via Docker
docker exec -it postgres-order psql -U order_user -d order_db

# Via local client
psql -h localhost -p 5433 -U order_user -d order_db
```

---

### Apache Kafka

Message broker for event-driven communication.

| Component | Container | Port | Description |
|-----------|-----------|------|-------------|
| Zookeeper | zookeeper | 2181 | Kafka coordination |
| Kafka | kafka | 9092 (external), 29092 (internal) | Message broker |
| Kafka UI | kafka-ui | 8090 | Web interface |

**Access Kafka UI:** http://localhost:8090

**Create topic manually:**

```bash
docker exec -it kafka kafka-topics --create \
  --topic test-topic \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1
```

**List topics:**

```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

---

### Redis

In-memory cache for API Gateway rate limiting.

| Container | Port | Purpose |
|-----------|------|---------|
| redis | 6379 | Rate limiting, caching |

**Connect to Redis:**

```bash
docker exec -it redis redis-cli
> PING
PONG
> KEYS *
```

---

### Keycloak

Identity and Access Management server.

| Container | Port | Purpose |
|-----------|------|---------|
| keycloak | 8180 | OAuth2/OIDC Provider |

**Access Admin Console:** http://localhost:8180

**Credentials:**
- Username: `admin`
- Password: `admin`

---

## Microservices

### Container Configuration

Each microservice uses multi-stage Docker builds:

```dockerfile
# Build stage - Full JDK for compilation
FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Runtime stage - Minimal JRE
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Service Ports

| Service | Container Port | Host Port |
|---------|---------------|-----------|
| API Gateway | 8080 | 8080 |
| Order Service | 8081 | 8081 |
| Restaurant Service | 8082 | 8082 |
| User Service | 8083 | 8083 |
| Delivery Service | 8084 | 8084 |

---

## Environment Variables

### Common Variables

```bash
# Spring Profile
SPRING_PROFILES_ACTIVE=docker

# OAuth2/JWT
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://keycloak:8080/realms/delivery-realm
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://keycloak:8080/realms/delivery-realm/protocol/openid-connect/certs

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

### Service-Specific Variables

**Order Service:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-order:5432/order_db
SPRING_DATASOURCE_USERNAME=order_user
SPRING_DATASOURCE_PASSWORD=order_password
```

**Restaurant Service:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-restaurant:5432/restaurant_db
SPRING_DATASOURCE_USERNAME=restaurant_user
SPRING_DATASOURCE_PASSWORD=restaurant_password
```

**User Service:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-user:5432/user_db
SPRING_DATASOURCE_USERNAME=user_user
SPRING_DATASOURCE_PASSWORD=user_password
```

**Delivery Service:**
```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-delivery:5432/delivery_db
SPRING_DATASOURCE_USERNAME=delivery_user
SPRING_DATASOURCE_PASSWORD=delivery_password
```

**API Gateway:**
```bash
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379
ORDER_SERVICE_URL=http://order-service:8081
RESTAURANT_SERVICE_URL=http://restaurant-service:8082
USER_SERVICE_URL=http://user-service:8083
DELIVERY_SERVICE_URL=http://delivery-service:8084
```

---

## Volumes & Persistence

### Named Volumes

All data is persisted using Docker named volumes:

```yaml
volumes:
  postgres-order-data:      # Order database
  postgres-restaurant-data: # Restaurant database
  postgres-user-data:       # User database
  postgres-delivery-data:   # Delivery database
  postgres-keycloak-data:   # Keycloak database
  zookeeper-data:           # Zookeeper data
  zookeeper-logs:           # Zookeeper logs
  kafka-data:               # Kafka messages
  redis-data:               # Redis cache
```

### Volume Management

```bash
# List volumes
docker volume ls | grep javadeliveryproject

# Inspect volume
docker volume inspect javadeliveryproject_postgres-order-data

# Backup database volume
docker run --rm -v javadeliveryproject_postgres-order-data:/data -v $(pwd):/backup alpine \
  tar cvf /backup/postgres-order-backup.tar /data

# Remove all volumes (WARNING: Data loss!)
docker compose down -v
```

---

## Networking

### Docker Network

All containers communicate via the `delivery-network` bridge network.

```yaml
networks:
  delivery-network:
    driver: bridge
```

### Internal DNS

Within Docker, services reference each other by container name:

| Service | Internal Hostname |
|---------|------------------|
| Order DB | postgres-order |
| Restaurant DB | postgres-restaurant |
| User DB | postgres-user |
| Delivery DB | postgres-delivery |
| Kafka | kafka |
| Redis | redis |
| Keycloak | keycloak |
| Order Service | order-service |
| Restaurant Service | restaurant-service |
| User Service | user-service |
| Delivery Service | delivery-service |

### Port Mapping Summary

```
Host Machine Ports:
├── 8080  → API Gateway
├── 8081  → Order Service
├── 8082  → Restaurant Service
├── 8083  → User Service
├── 8084  → Delivery Service
├── 8090  → Kafka UI
├── 8180  → Keycloak
├── 9092  → Kafka (external)
├── 5433  → PostgreSQL (Order)
├── 5434  → PostgreSQL (Restaurant)
├── 5435  → PostgreSQL (User)
├── 5436  → PostgreSQL (Delivery)
├── 6379  → Redis
└── 2181  → Zookeeper
```

---

## Building Images

### Build All Services

```bash
# Build all services
docker compose -f docker-compose.full.yml build

# Build specific service
docker compose -f docker-compose.full.yml build order-service

# Build without cache
docker compose -f docker-compose.full.yml build --no-cache
```

### Build and Tag for Registry

```bash
# Build with custom tag
docker build -t myregistry/order-service:1.0.0 ./order-service

# Push to registry
docker push myregistry/order-service:1.0.0
```

---

## Useful Commands

### Lifecycle Commands

```bash
# Start all services
docker compose up -d

# Stop all services
docker compose stop

# Stop and remove containers
docker compose down

# Stop and remove containers + volumes
docker compose down -v

# Restart specific service
docker compose restart order-service

# Rebuild and restart
docker compose up -d --build order-service
```

### Monitoring Commands

```bash
# View all container status
docker compose ps

# View logs (all services)
docker compose logs -f

# View logs (specific service)
docker compose logs -f order-service

# View logs with timestamps
docker compose logs -f --timestamps order-service

# View last 100 lines
docker compose logs --tail=100 order-service
```

### Debugging Commands

```bash
# Execute command in container
docker exec -it order-service sh

# Check container health
docker inspect --format='{{.State.Health.Status}}' order-service

# View resource usage
docker stats --no-stream

# View container processes
docker top order-service

# Copy file from container
docker cp order-service:/app/logs/app.log ./app.log
```

### Cleanup Commands

```bash
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune

# Remove everything unused
docker system prune -a --volumes

# View disk usage
docker system df
```

---

## Production Considerations

### Security

1. **Change default passwords:**
```yaml
environment:
  POSTGRES_PASSWORD: ${DB_PASSWORD}  # Use secrets
  KEYCLOAK_ADMIN_PASSWORD: ${KC_ADMIN_PASSWORD}
```

2. **Use Docker secrets:**
```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt

services:
  postgres-order:
    secrets:
      - db_password
    environment:
      POSTGRES_PASSWORD_FILE: /run/secrets/db_password
```

3. **Remove Kafka UI in production:**
```yaml
# Comment out or remove kafka-ui service
```

### Resource Limits

```yaml
services:
  order-service:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### Health Checks

All services include health checks. Ensure `start_period` is sufficient:

```yaml
healthcheck:
  test: ["CMD", "wget", "--spider", "http://localhost:8081/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 120s  # Increase for slow startup
```

### Logging

Configure centralized logging:

```yaml
services:
  order-service:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### Scaling

For horizontal scaling, use Docker Swarm or Kubernetes:

```bash
# Docker Compose scaling (limited)
docker compose up -d --scale order-service=3

# Note: Requires load balancer configuration
```

---

## Common Issues

### Container Startup Order

Services may fail if dependencies aren't ready. Use `depends_on` with health checks:

```yaml
order-service:
  depends_on:
    postgres-order:
      condition: service_healthy
    kafka:
      condition: service_healthy
```

### Memory Issues

If containers are killed by OOM:

```bash
# Check logs for OOM
docker logs order-service 2>&1 | grep -i "killed"

# Increase Docker memory limit (Docker Desktop settings)
# Or add JVM limits:
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
```

### Network Connectivity

If services can't communicate:

```bash
# Verify network exists
docker network ls

# Check container is on network
docker network inspect delivery-network

# Test connectivity
docker exec -it order-service ping postgres-order
```

---

## Quick Reference Card

```bash
# Start everything
docker compose -f docker-compose.full.yml up -d

# View status
docker compose ps

# View logs
docker compose logs -f

# Stop everything
docker compose down

# Clean restart
docker compose down -v && docker compose -f docker-compose.full.yml up -d --build

# Access services
# API Gateway:    http://localhost:8080
# Keycloak:       http://localhost:8180
# Kafka UI:       http://localhost:8090
# Swagger (Order): http://localhost:8081/swagger-ui.html
```
