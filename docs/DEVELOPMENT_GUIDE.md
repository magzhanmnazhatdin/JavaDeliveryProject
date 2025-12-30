# Development Guide

Complete guide for developers working on the Food Delivery Microservices Platform.

---

## Table of Contents

- [Development Environment Setup](#development-environment-setup)
- [Project Structure](#project-structure)
- [Coding Standards](#coding-standards)
- [Creating a New Microservice](#creating-a-new-microservice)
- [Adding New Endpoints](#adding-new-endpoints)
- [Working with Kafka Events](#working-with-kafka-events)
- [Database Migrations](#database-migrations)
- [Testing](#testing)
- [Debugging](#debugging)
- [IDE Configuration](#ide-configuration)

---

## Development Environment Setup

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 17+ | Runtime |
| Maven | 3.9+ | Build tool |
| Docker | 20.10+ | Containers |
| Docker Compose | 2.0+ | Orchestration |
| IDE | IntelliJ IDEA / VS Code | Development |
| Git | 2.30+ | Version control |

### Initial Setup

```bash
# Clone repository
git clone <repository-url>
cd JavaDeliveryProject

# Start infrastructure
docker compose up -d

# Wait for services to be healthy
docker compose ps

# Build all services
./mvnw clean install -DskipTests

# Verify build
./mvnw test
```

### Running Services Locally

**Option 1: Run Single Service**

```bash
cd order-service
./mvnw spring-boot:run
```

**Option 2: Run with Debug**

```bash
cd order-service
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

**Option 3: Run All Services (separate terminals)**

```bash
# Terminal 1
cd order-service && ./mvnw spring-boot:run

# Terminal 2
cd user-service && ./mvnw spring-boot:run

# Terminal 3
cd restaurant-service && ./mvnw spring-boot:run

# Terminal 4
cd delivery-service && ./mvnw spring-boot:run

# Terminal 5
cd api-gateway && ./mvnw spring-boot:run
```

---

## Project Structure

### Overall Layout

```
JavaDeliveryProject/
├── api-gateway/           # API Gateway service
├── order-service/         # Order & Payment management
├── restaurant-service/    # Restaurant & Menu management
├── user-service/          # User management
├── delivery-service/      # Delivery & Courier management
├── docs/                  # Documentation
├── keycloak/             # Keycloak configuration
├── docker-compose.yml     # Infrastructure
├── docker-compose.services.yml
├── docker-compose.full.yml
├── .env                   # Environment variables
└── README.md
```

### Service Structure

Each microservice follows this structure:

```
<service-name>/
├── src/
│   ├── main/
│   │   ├── java/com/example/<service>/
│   │   │   ├── config/           # Configuration classes
│   │   │   ├── controller/       # REST controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   │   ├── request/      # Request DTOs
│   │   │   │   └── response/     # Response DTOs
│   │   │   ├── entity/           # JPA entities
│   │   │   ├── exception/        # Custom exceptions
│   │   │   ├── kafka/            # Kafka producers/consumers
│   │   │   ├── mapper/           # Entity <-> DTO mappers
│   │   │   ├── repository/       # Spring Data repositories
│   │   │   ├── service/          # Business logic
│   │   │   └── <Service>Application.java
│   │   └── resources/
│   │       ├── db/migration/     # Flyway migrations
│   │       ├── application.yml   # Default config
│   │       └── application-docker.yml
│   └── test/
│       ├── java/                 # Test classes
│       └── resources/
│           └── application-test.yml
├── Dockerfile
├── pom.xml
└── HELP.md
```

---

## Coding Standards

### Package Naming

```
com.example.<service>           # Base package
com.example.<service>.entity    # JPA entities
com.example.<service>.dto       # DTOs
com.example.<service>.service   # Services
com.example.<service>.controller # Controllers
```

### Class Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Entity | Singular noun | `Order`, `User`, `Restaurant` |
| Repository | Entity + Repository | `OrderRepository` |
| Service | Entity + Service | `OrderService` |
| Controller | Entity + Controller | `OrderController` |
| DTO | Entity + Dto | `OrderDto`, `CreateOrderRequest` |
| Mapper | Entity + Mapper | `OrderMapper` |
| Exception | Descriptive + Exception | `OrderNotFoundException` |

### Code Style

```java
// Use Lombok for boilerplate reduction
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();
}
```

### Service Layer Pattern

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final OrderEventProducer eventProducer;

    @Transactional
    public OrderDto createOrder(CreateOrderRequest request, String customerId) {
        log.info("Creating order for customer: {}", customerId);

        Order order = orderMapper.toEntity(request);
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);

        eventProducer.sendOrderCreatedEvent(savedOrder);

        return orderMapper.toDto(savedOrder);
    }
}
```

### Controller Pattern

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Create new order")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Order created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        String customerId = extractUserId(authentication);
        OrderDto order = orderService.createOrder(request, customerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    private String extractUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
```

---

## Creating a New Microservice

### Step 1: Create Maven Module

```bash
# Create directory structure
mkdir -p new-service/src/main/java/com/example/newservice
mkdir -p new-service/src/main/resources/db/migration
mkdir -p new-service/src/test/java/com/example/newservice
mkdir -p new-service/src/test/resources
```

### Step 2: Create pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.0</version>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>new-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>new-service</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>

        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>

        <!-- Utilities -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.3.0</version>
        </dependency>

        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Step 3: Create Application Class

```java
package com.example.newservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NewServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewServiceApplication.class, args);
    }
}
```

### Step 4: Create Configuration Files

**application.yml:**
```yaml
server:
  port: 8085

spring:
  application:
    name: new-service

  datasource:
    url: jdbc:postgresql://localhost:5437/new_db
    username: new_user
    password: new_password
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

  flyway:
    enabled: true
    locations: classpath:db/migration

  kafka:
    bootstrap-servers: localhost:9092

  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/delivery-realm

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  level:
    com.example.newservice: DEBUG
```

### Step 5: Add Security Config

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());
        return converter;
    }
}
```

### Step 6: Create Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk as build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -g 1001 -S appgroup && adduser -u 1001 -S appuser -G appgroup
COPY --from=build /app/target/*.jar app.jar
RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8085
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Step 7: Add to Docker Compose

Add database to `docker-compose.yml`:
```yaml
postgres-new:
  image: postgres:15-alpine
  container_name: postgres-new
  environment:
    POSTGRES_DB: new_db
    POSTGRES_USER: new_user
    POSTGRES_PASSWORD: new_password
  ports:
    - "5437:5432"
  volumes:
    - postgres-new-data:/var/lib/postgresql/data
  networks:
    - delivery-network
```

Add service to `docker-compose.services.yml`:
```yaml
new-service:
  build:
    context: ./new-service
    dockerfile: Dockerfile
  container_name: new-service
  ports:
    - "8085:8085"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-new:5432/new_db
    # ... other env vars
  networks:
    - delivery-network
```

---

## Adding New Endpoints

### Step 1: Create Entity

```java
@Entity
@Table(name = "items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
```

### Step 2: Create DTOs

```java
// Request DTO
@Data
@Builder
public class CreateItemRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;
}

// Response DTO
@Data
@Builder
public class ItemDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private LocalDateTime createdAt;
}
```

### Step 3: Create Repository

```java
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByNameContainingIgnoreCase(String name);

    @Query("SELECT i FROM Item i WHERE i.price BETWEEN :minPrice AND :maxPrice")
    List<Item> findByPriceRange(@Param("minPrice") BigDecimal min,
                                 @Param("maxPrice") BigDecimal max);
}
```

### Step 4: Create Mapper

```java
@Component
public class ItemMapper {

    public Item toEntity(CreateItemRequest request) {
        return Item.builder()
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .build();
    }

    public ItemDto toDto(Item item) {
        return ItemDto.builder()
            .id(item.getId())
            .name(item.getName())
            .description(item.getDescription())
            .price(item.getPrice())
            .createdAt(item.getCreatedAt())
            .build();
    }

    public List<ItemDto> toDtoList(List<Item> items) {
        return items.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
}
```

### Step 5: Create Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemMapper itemMapper;

    public ItemDto createItem(CreateItemRequest request) {
        log.info("Creating item: {}", request.getName());
        Item item = itemMapper.toEntity(request);
        Item saved = itemRepository.save(item);
        return itemMapper.toDto(saved);
    }

    public ItemDto getItemById(Long id) {
        Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ItemNotFoundException("Item not found: " + id));
        return itemMapper.toDto(item);
    }

    public List<ItemDto> getAllItems() {
        return itemMapper.toDtoList(itemRepository.findAll());
    }

    @Transactional
    public ItemDto updateItem(Long id, CreateItemRequest request) {
        Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ItemNotFoundException("Item not found: " + id));

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setPrice(request.getPrice());

        return itemMapper.toDto(itemRepository.save(item));
    }

    public void deleteItem(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ItemNotFoundException("Item not found: " + id);
        }
        itemRepository.deleteById(id);
    }
}
```

### Step 6: Create Controller

```java
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@Tag(name = "Items", description = "Item management API")
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto createItem(@Valid @RequestBody CreateItemRequest request) {
        return itemService.createItem(request);
    }

    @GetMapping("/{id}")
    public ItemDto getItem(@PathVariable Long id) {
        return itemService.getItemById(id);
    }

    @GetMapping
    public List<ItemDto> getAllItems() {
        return itemService.getAllItems();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItemDto updateItem(@PathVariable Long id,
                               @Valid @RequestBody CreateItemRequest request) {
        return itemService.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
    }
}
```

### Step 7: Add Database Migration

Create `src/main/resources/db/migration/V1__create_items_table.sql`:

```sql
CREATE TABLE items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_items_name ON items(name);
```

---

## Working with Kafka Events

### Creating Event Producer

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "item-events";

    public void sendItemCreatedEvent(Item item) {
        ItemEvent event = ItemEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("ITEM_CREATED")
            .itemId(item.getId())
            .name(item.getName())
            .price(item.getPrice())
            .timestamp(LocalDateTime.now())
            .build();

        kafkaTemplate.send(TOPIC, item.getId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Sent event: {} to topic: {}", event.getEventType(), TOPIC);
                } else {
                    log.error("Failed to send event", ex);
                }
            });
    }
}
```

### Creating Event Consumer

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ItemEventsListener {

    private final ItemService itemService;

    @KafkaListener(topics = "item-events", groupId = "new-service")
    public void handleItemEvent(ItemEvent event) {
        log.info("Received event: {} for item: {}",
            event.getEventType(), event.getItemId());

        switch (event.getEventType()) {
            case "ITEM_CREATED":
                handleItemCreated(event);
                break;
            case "ITEM_UPDATED":
                handleItemUpdated(event);
                break;
            default:
                log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleItemCreated(ItemEvent event) {
        // Process item created event
    }

    private void handleItemUpdated(ItemEvent event) {
        // Process item updated event
    }
}
```

### Kafka Configuration

```java
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "new-service");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new DefaultKafkaConsumerFactory<>(config);
    }
}
```

---

## Database Migrations

### Flyway Naming Convention

```
V<version>__<description>.sql

Examples:
V1__create_items_table.sql
V2__add_category_column.sql
V3__create_categories_table.sql
```

### Migration Examples

**Add column:**
```sql
-- V2__add_category_column.sql
ALTER TABLE items ADD COLUMN category VARCHAR(100);
```

**Add foreign key:**
```sql
-- V3__add_category_relation.sql
ALTER TABLE items
ADD CONSTRAINT fk_items_category
FOREIGN KEY (category_id) REFERENCES categories(id);
```

**Add index:**
```sql
-- V4__add_indexes.sql
CREATE INDEX idx_items_category ON items(category_id);
CREATE INDEX idx_items_price ON items(price);
```

---

## Testing

### Unit Test Example

```java
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemMapper itemMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void createItem_shouldReturnCreatedItem() {
        // Given
        CreateItemRequest request = CreateItemRequest.builder()
            .name("Test Item")
            .price(BigDecimal.valueOf(10.00))
            .build();

        Item item = Item.builder().id(1L).name("Test Item").build();
        ItemDto expectedDto = ItemDto.builder().id(1L).name("Test Item").build();

        when(itemMapper.toEntity(request)).thenReturn(item);
        when(itemRepository.save(item)).thenReturn(item);
        when(itemMapper.toDto(item)).thenReturn(expectedDto);

        // When
        ItemDto result = itemService.createItem(request);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Item");
        verify(itemRepository).save(item);
    }

    @Test
    void getItemById_whenNotFound_shouldThrowException() {
        // Given
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ItemNotFoundException.class,
            () -> itemService.getItemById(999L));
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createItem_shouldReturn201() throws Exception {
        CreateItemRequest request = CreateItemRequest.builder()
            .name("Test Item")
            .price(BigDecimal.valueOf(10.00))
            .build();

        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void createItem_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ItemServiceTest

# Run with coverage
./mvnw test jacoco:report
```

---

## Debugging

### Enable Debug Logging

```yaml
logging:
  level:
    root: INFO
    com.example: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### Remote Debugging

```bash
# Start with debug port
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

In IDE, create Remote Debug configuration pointing to `localhost:5005`.

### Actuator Endpoints

```bash
# Health
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/metrics

# Environment
curl http://localhost:8081/actuator/env

# Beans
curl http://localhost:8081/actuator/beans
```

---

## IDE Configuration

### IntelliJ IDEA

1. **Import Project:**
   - File → Open → Select project directory
   - Import as Maven project

2. **Enable Annotation Processing:**
   - Settings → Build → Compiler → Annotation Processors
   - Enable annotation processing

3. **Configure Code Style:**
   - Settings → Editor → Code Style → Java
   - Set indent to 4 spaces

4. **Add Run Configurations:**
   - For each service, create Spring Boot run configuration
   - Set active profile to `default`

### VS Code

1. **Required Extensions:**
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - Lombok Annotations Support

2. **Settings (settings.json):**
```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "spring-boot.ls.java.vmargs": "-Xmx1024m"
}
```

---

## Best Practices Checklist

- [ ] Follow package naming conventions
- [ ] Use Lombok for reducing boilerplate
- [ ] Create separate DTOs for requests and responses
- [ ] Add validation annotations to request DTOs
- [ ] Use `@Transactional` for data-modifying operations
- [ ] Add proper logging with SLF4J
- [ ] Document APIs with OpenAPI annotations
- [ ] Write unit tests for services
- [ ] Write integration tests for controllers
- [ ] Use Flyway for database migrations
- [ ] Handle exceptions with `@ControllerAdvice`
- [ ] Use proper HTTP status codes
- [ ] Secure endpoints with `@PreAuthorize`
