package com.example.orderservice.kafka;

import com.example.orderservice.dto.event.OrderCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        },
        topics = {"order-events", "restaurant-events", "delivery-events"}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class OrderEventProducerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private OrderEventProducer orderEventProducer;

    @Autowired
    private ObjectMapper objectMapper;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group", "true", embeddedKafkaBroker
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        consumer = consumerFactory.createConsumer();
        consumer.subscribe(List.of("order-events"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    @DisplayName("Should send OrderCreated event to Kafka topic")
    void sendOrderCreatedEvent_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .eventType("OrderCreated")
                .orderId(orderId)
                .customerId(customerId)
                .restaurantId(restaurantId)
                .totalPrice(BigDecimal.valueOf(49.99))
                .deliveryAddress("456 Oak Avenue")
                .items(List.of(
                        OrderCreatedEvent.OrderItemEvent.builder()
                                .menuItemId(UUID.randomUUID())
                                .name("Burger")
                                .quantity(2)
                                .price(BigDecimal.valueOf(15.99))
                                .build()
                ))
                .createdAt(Instant.now())
                .build();

        orderEventProducer.sendOrderCreatedEvent(event);

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                consumer, "order-events", Duration.ofSeconds(10)
        );

        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(orderId.toString());

        OrderCreatedEvent receivedEvent = objectMapper.readValue(record.value(), OrderCreatedEvent.class);
        assertThat(receivedEvent.getEventType()).isEqualTo("OrderCreated");
        assertThat(receivedEvent.getOrderId()).isEqualTo(orderId);
        assertThat(receivedEvent.getCustomerId()).isEqualTo(customerId);
        assertThat(receivedEvent.getRestaurantId()).isEqualTo(restaurantId);
        assertThat(receivedEvent.getTotalPrice()).isEqualByComparingTo(BigDecimal.valueOf(49.99));
        assertThat(receivedEvent.getDeliveryAddress()).isEqualTo("456 Oak Avenue");
        assertThat(receivedEvent.getItems()).hasSize(1);
    }

    @Test
    @DisplayName("Should send multiple order events")
    void sendMultipleOrderEvents_Success() throws Exception {
        for (int i = 0; i < 3; i++) {
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .eventType("OrderCreated")
                    .orderId(UUID.randomUUID())
                    .customerId(UUID.randomUUID())
                    .restaurantId(UUID.randomUUID())
                    .totalPrice(BigDecimal.valueOf(10.00 * (i + 1)))
                    .deliveryAddress("Address " + i)
                    .items(List.of())
                    .createdAt(Instant.now())
                    .build();

            orderEventProducer.sendOrderCreatedEvent(event);
        }

        var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));

        assertThat(records.count()).isGreaterThanOrEqualTo(3);
    }
}
