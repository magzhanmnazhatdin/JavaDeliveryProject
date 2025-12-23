package com.example.restaurantservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.restaurant-events}")
    private String restaurantEventsTopic;

    @Bean
    public NewTopic restaurantEventsTopic() {
        return TopicBuilder.name(restaurantEventsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
