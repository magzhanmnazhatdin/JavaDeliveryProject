package com.example.userservice.kafka;

import com.example.userservice.entity.User;
import com.example.userservice.entity.UserRole;
import com.example.userservice.entity.UserStatus;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9094", "port=9094"},
        topics = {"user-events-test"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class UserEventProducerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private UserEventProducer userEventProducer;

    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;

    private Consumer<String, Map<String, Object>> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.HashMap");

        DefaultKafkaConsumerFactory<String, Map<String, Object>> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        consumer = consumerFactory.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, userEventsTopic);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should send USER_CREATED event to Kafka")
    void sendUserCreatedEvent_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .keycloakId(UUID.randomUUID().toString())
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        userEventProducer.sendUserCreatedEvent(user);

        ConsumerRecord<String, Map<String, Object>> record =
                KafkaTestUtils.getSingleRecord(consumer, userEventsTopic);

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(userId.toString());

        Map<String, Object> event = record.value();
        assertThat(event.get("eventType")).isEqualTo("USER_CREATED");
        assertThat(event.get("email")).isEqualTo("test@example.com");
        assertThat(event.get("firstName")).isEqualTo("John");
        assertThat(event.get("lastName")).isEqualTo("Doe");
        assertThat(event.get("role")).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("Should send USER_STATUS_CHANGED event to Kafka")
    void sendUserStatusChangedEvent_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .keycloakId(UUID.randomUUID().toString())
                .email("test@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.SUSPENDED)
                .createdAt(Instant.now())
                .build();

        userEventProducer.sendUserStatusChangedEvent(user, UserStatus.ACTIVE, UserStatus.SUSPENDED);

        ConsumerRecord<String, Map<String, Object>> record =
                KafkaTestUtils.getSingleRecord(consumer, userEventsTopic);

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(userId.toString());

        Map<String, Object> event = record.value();
        assertThat(event.get("eventType")).isEqualTo("USER_STATUS_CHANGED");
        assertThat(event.get("previousStatus")).isEqualTo("ACTIVE");
        assertThat(event.get("newStatus")).isEqualTo("SUSPENDED");
    }

    @Test
    @DisplayName("Should send USER_UPDATED event to Kafka")
    void sendUserUpdatedEvent_Success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .keycloakId(UUID.randomUUID().toString())
                .email("updated@example.com")
                .firstName("Updated")
                .lastName("User")
                .phone("+1234567890")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        userEventProducer.sendUserUpdatedEvent(user);

        ConsumerRecord<String, Map<String, Object>> record =
                KafkaTestUtils.getSingleRecord(consumer, userEventsTopic);

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(userId.toString());

        Map<String, Object> event = record.value();
        assertThat(event.get("eventType")).isEqualTo("USER_UPDATED");
        assertThat(event.get("email")).isEqualTo("updated@example.com");
        assertThat(event.get("firstName")).isEqualTo("Updated");
        assertThat(event.get("phone")).isEqualTo("+1234567890");
    }
}
