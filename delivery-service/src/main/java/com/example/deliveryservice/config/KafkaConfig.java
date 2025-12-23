package com.example.deliveryservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.delivery-events}")
    private String deliveryEventsTopic;

    @Bean
    public NewTopic deliveryEventsTopic() {
        return TopicBuilder.name(deliveryEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
