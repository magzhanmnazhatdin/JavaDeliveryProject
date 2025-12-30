# Troubleshooting Guide

This guide covers common issues and their solutions when working with the Food Delivery Microservices Platform.

---

## Table of Contents

- [Docker & Infrastructure](#docker--infrastructure)
- [Database Issues](#database-issues)
- [Kafka Issues](#kafka-issues)
- [Keycloak & Authentication](#keycloak--authentication)
- [API Gateway](#api-gateway)
- [Microservices](#microservices)
- [Network Issues](#network-issues)
- [Performance Issues](#performance-issues)
- [Logging & Debugging](#logging--debugging)

---

## Docker & Infrastructure

### Containers Won't Start

**Symptoms:**
- `docker compose up` fails
- Services exit immediately after starting

**Solutions:**

1. **Check Docker resources:**
```bash
# Ensure Docker has enough memory (recommended: 8GB+)
docker system info | grep -i memory
```

2. **Remove old containers and volumes:**
```bash
docker compose down -v
docker system prune -a
docker compose up -d
```

3. **Check port conflicts:**
```bash
# Windows
netstat -ano | findstr "8080 8081 8082 8083 8084 5433 5434 5435 5436 9092 8180 6379"

# Linux/Mac
lsof -i :8080 -i :8081 -i :8082 -i :8083 -i :8084
```

4. **Kill conflicting processes:**
```bash
# Windows - find and kill process by PID
taskkill /PID <PID> /F

# Linux/Mac
kill -9 <PID>
```

---

### Container Health Check Fails

**Symptoms:**
- Service shows `unhealthy` status
- Dependencies don't start

**Solutions:**

1. **Check container logs:**
```bash
docker logs <container-name> --tail 100
docker logs order-service --tail 100
```

2. **Manually test health endpoint:**
```bash
curl http://localhost:8081/actuator/health
```

3. **Increase start period in docker-compose:**
```yaml
healthcheck:
  start_period: 120s  # Increase from 60s
```

4. **Check service dependencies:**
```bash
docker compose ps
# Ensure all required services are healthy before starting dependents
```

---

### Out of Disk Space

**Symptoms:**
- Build fails
- Cannot create new containers

**Solutions:**

```bash
# Check disk usage
docker system df

# Clean up unused resources
docker system prune -a --volumes

# Remove dangling images
docker image prune -a

# Remove unused volumes
docker volume prune
```

---

## Database Issues

### Connection Refused

**Symptoms:**
```
Connection refused to localhost:5433
FATAL: password authentication failed for user "order_user"
```

**Solutions:**

1. **Check database is running:**
```bash
docker ps | grep postgres
docker logs postgres-order
```

2. **Verify credentials in .env:**
```bash
# Check .env matches docker-compose.yml
ORDER_DB_USER=order_user
ORDER_DB_PASSWORD=order_password
```

3. **Test database connection:**
```bash
docker exec -it postgres-order psql -U order_user -d order_db
```

4. **Reset database volume:**
```bash
docker compose down
docker volume rm javadeliveryproject_postgres-order-data
docker compose up -d postgres-order
```

---

### Flyway Migration Fails

**Symptoms:**
```
Migration checksum mismatch
Flyway migration failed
```

**Solutions:**

1. **Clean and re-run migrations (development only):**
```bash
# Add to application.yml temporarily
spring:
  flyway:
    clean-disabled: false

# Or via environment variable
SPRING_FLYWAY_CLEAN_DISABLED=false
```

2. **Repair Flyway history:**
```bash
# Connect to database
docker exec -it postgres-order psql -U order_user -d order_db

# Clear flyway history
DELETE FROM flyway_schema_history WHERE success = false;
```

3. **Reset database completely:**
```bash
docker exec -it postgres-order psql -U order_user -d order_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

---

### Database Queries Slow

**Solutions:**

1. **Check for missing indexes:**
```sql
-- Connect to database
docker exec -it postgres-order psql -U order_user -d order_db

-- Analyze query
EXPLAIN ANALYZE SELECT * FROM orders WHERE customer_id = 'xxx';
```

2. **Add indexes if needed:**
```sql
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
```

3. **Update PostgreSQL statistics:**
```sql
VACUUM ANALYZE;
```

---

## Kafka Issues

### Kafka Won't Start

**Symptoms:**
- Kafka container keeps restarting
- "Zookeeper not available"

**Solutions:**

1. **Ensure Zookeeper is healthy first:**
```bash
docker logs zookeeper
docker exec -it zookeeper nc -z localhost 2181
```

2. **Restart Kafka after Zookeeper:**
```bash
docker compose restart zookeeper
# Wait 30 seconds
docker compose restart kafka
```

3. **Check Kafka logs:**
```bash
docker logs kafka --tail 200
```

---

### Messages Not Being Consumed

**Symptoms:**
- Events published but not received
- Consumer lag increasing

**Solutions:**

1. **Check topic exists:**
```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

2. **Check consumer groups:**
```bash
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list

# Check consumer lag
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group restaurant-service
```

3. **Verify consumer is connected:**
```bash
# Check Kafka UI at http://localhost:8090
# Or via CLI
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --all-groups
```

4. **Reset consumer offset (if needed):**
```bash
docker exec -it kafka kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group restaurant-service \
  --topic order-events \
  --reset-offsets --to-earliest --execute
```

---

### Kafka UI Not Working

**Symptoms:**
- Cannot access http://localhost:8090
- UI shows no data

**Solutions:**

1. **Check Kafka UI container:**
```bash
docker logs kafka-ui
```

2. **Verify Kafka connection:**
```bash
# Kafka UI uses internal network
docker exec -it kafka-ui wget -qO- kafka:29092 || echo "Cannot connect"
```

3. **Restart Kafka UI:**
```bash
docker compose restart kafka-ui
```

---

## Keycloak & Authentication

### 401 Unauthorized on All Requests

**Symptoms:**
- API returns 401 for authenticated requests
- Token validation fails

**Solutions:**

1. **Check token is valid:**
```bash
# Decode JWT at jwt.io or:
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq .
```

2. **Verify issuer-uri matches:**
```yaml
# In application.yml - ensure this matches your Keycloak URL
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/delivery-realm
```

3. **Check Keycloak is accessible:**
```bash
curl http://localhost:8180/realms/delivery-realm/.well-known/openid-configuration
```

4. **Verify token hasn't expired:**
```bash
# Check 'exp' claim in token
# Default expiry is 5 minutes
```

---

### Cannot Get Token from Keycloak

**Symptoms:**
- Token endpoint returns error
- "Invalid client credentials"

**Solutions:**

1. **Verify client exists and is enabled:**
   - Open http://localhost:8180
   - Login as admin/admin
   - Go to Clients → delivery-api
   - Ensure "Enabled" is ON

2. **Check client secret:**
   - Go to Clients → delivery-api → Credentials
   - Copy the secret and use in requests

3. **Verify user exists with password:**
   - Go to Users → select user
   - Credentials tab → ensure password is set

4. **Test with cURL:**
```bash
curl -X POST http://localhost:8180/realms/delivery-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=delivery-api" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=admin" \
  -d "password=admin123" \
  -v
```

---

### 403 Forbidden - Role Issues

**Symptoms:**
- Token works but access denied
- Roles not recognized

**Solutions:**

1. **Check user has correct roles:**
   - Keycloak → Users → select user → Role Mappings
   - Assign realm roles: ADMIN, CUSTOMER, RESTAURANT, COURIER

2. **Verify role converter is configured:**
```java
// Check KeycloakRealmRoleConverter is used in SecurityConfig
@Bean
public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
    return converter;
}
```

3. **Check token contains roles:**
```bash
# Decode token and check realm_access.roles
echo $TOKEN | cut -d'.' -f2 | base64 -d 2>/dev/null | jq '.realm_access.roles'
```

---

### Keycloak Not Accessible

**Symptoms:**
- Cannot open http://localhost:8180
- Services cannot validate tokens

**Solutions:**

1. **Check Keycloak container:**
```bash
docker logs keycloak --tail 100
docker ps | grep keycloak
```

2. **Wait for startup (takes 60-90 seconds):**
```bash
# Check health endpoint
curl http://localhost:8180/health/ready
```

3. **Check Keycloak database:**
```bash
docker logs postgres-keycloak
docker exec -it postgres-keycloak psql -U keycloak -d keycloak -c "\dt"
```

---

## API Gateway

### 503 Service Unavailable

**Symptoms:**
- Gateway returns 503
- Circuit breaker is open

**Solutions:**

1. **Check target service is running:**
```bash
curl http://localhost:8081/actuator/health
```

2. **Check circuit breaker status:**
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/circuitbreakers
```

3. **Wait for circuit breaker to close:**
   - Default wait time is 5 seconds
   - Or restart the gateway

4. **Check gateway routes:**
```bash
curl http://localhost:8080/actuator/gateway/routes
```

---

### 429 Too Many Requests

**Symptoms:**
- Rate limit exceeded
- Redis rate limiter blocking requests

**Solutions:**

1. **Check Redis is running:**
```bash
docker exec -it redis redis-cli ping
```

2. **Clear rate limit keys:**
```bash
docker exec -it redis redis-cli KEYS "*rate*"
docker exec -it redis redis-cli FLUSHDB
```

3. **Adjust rate limits in application.yml:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 50  # Increase
                redis-rate-limiter.burstCapacity: 100
```

---

### CORS Errors

**Symptoms:**
- Browser shows CORS error
- Preflight request fails

**Solutions:**

1. **Check CORS configuration in gateway:**
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:3000"
              - "http://localhost:4200"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true
```

2. **Add your frontend origin:**
```yaml
allowedOrigins:
  - "http://your-frontend-url:port"
```

---

## Microservices

### Service Won't Start

**Symptoms:**
- Application fails to start
- Bean creation errors

**Solutions:**

1. **Check application logs:**
```bash
# Docker
docker logs order-service --tail 200

# Local
./mvnw spring-boot:run 2>&1 | tee app.log
```

2. **Verify all dependencies are accessible:**
   - Database
   - Kafka
   - Keycloak

3. **Check for port conflicts:**
```bash
# Windows
netstat -ano | findstr "8081"
```

4. **Run with debug logging:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--logging.level.root=DEBUG"
```

---

### Inter-Service Communication Fails

**Symptoms:**
- WebClient calls fail
- Timeout errors

**Solutions:**

1. **Check target service health:**
```bash
curl http://localhost:8082/actuator/health
```

2. **Verify service URLs in configuration:**
```yaml
# Check application.yml or environment variables
restaurant-service:
  url: http://localhost:8082
```

3. **Increase timeout:**
```java
WebClient webClient = WebClient.builder()
    .baseUrl("http://localhost:8082")
    .clientConnector(new ReactorClientHttpConnector(
        HttpClient.create().responseTimeout(Duration.ofSeconds(30))))
    .build();
```

---

## Network Issues

### Cannot Connect Between Containers

**Symptoms:**
- Services cannot reach each other
- Connection refused in Docker

**Solutions:**

1. **Use Docker network hostnames:**
```yaml
# In Docker, use service names, not localhost
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-order:5432/order_db
# NOT: jdbc:postgresql://localhost:5433/order_db
```

2. **Check all services are on same network:**
```bash
docker network inspect javadeliveryproject_delivery-network
```

3. **Verify DNS resolution:**
```bash
docker exec -it order-service nslookup postgres-order
```

---

### Localhost vs Docker Hostnames

| Environment | Database URL | Kafka | Keycloak |
|-------------|--------------|-------|----------|
| Local (IDE) | `localhost:5433` | `localhost:9092` | `localhost:8180` |
| Docker | `postgres-order:5432` | `kafka:29092` | `keycloak:8080` |

---

## Performance Issues

### High Memory Usage

**Solutions:**

1. **Set JVM memory limits:**
```dockerfile
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
```

2. **Enable JVM container awareness:**
```dockerfile
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

3. **Monitor with Actuator:**
```bash
curl http://localhost:8081/actuator/metrics/jvm.memory.used
```

---

### Slow Startup

**Solutions:**

1. **Enable lazy initialization:**
```yaml
spring:
  main:
    lazy-initialization: true
```

2. **Use Spring Boot DevTools for local development:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## Logging & Debugging

### Enable Debug Logging

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.example: DEBUG
    org.springframework.security: DEBUG
    org.springframework.kafka: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### View Real-time Logs

```bash
# Docker containers
docker logs -f order-service

# All services
docker compose logs -f

# Specific service with timestamps
docker compose logs -f --timestamps order-service
```

### Check Actuator Endpoints

```bash
# Health details
curl http://localhost:8081/actuator/health | jq

# All metrics
curl http://localhost:8081/actuator/metrics

# Specific metric
curl http://localhost:8081/actuator/metrics/http.server.requests

# Environment
curl http://localhost:8081/actuator/env

# Beans
curl http://localhost:8081/actuator/beans
```

---

## Quick Diagnostic Commands

```bash
# Check all services health
for port in 8080 8081 8082 8083 8084; do
  echo "Port $port: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:$port/actuator/health)"
done

# Check all containers
docker compose ps

# View resource usage
docker stats --no-stream

# Check network connectivity
docker exec -it api-gateway wget -qO- http://order-service:8081/actuator/health

# Full system status
docker compose logs --tail=50
```

---

## Getting Help

If issues persist:

1. Check logs with DEBUG level enabled
2. Verify all configuration matches documentation
3. Ensure all services and dependencies are healthy
4. Review recent changes that might have caused the issue
5. Check GitHub issues or create a new one with:
   - Error messages
   - Steps to reproduce
   - Environment details (OS, Docker version, Java version)
