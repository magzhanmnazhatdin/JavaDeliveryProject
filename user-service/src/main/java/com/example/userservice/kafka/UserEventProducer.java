package com.example.userservice.kafka;

import com.example.userservice.dto.event.UserCreatedEvent;
import com.example.userservice.dto.event.UserStatusChangedEvent;
import com.example.userservice.dto.event.UserUpdatedEvent;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.user-events}")
    private String userEventsTopic;

    public void sendUserCreatedEvent(User user) {
        UserCreatedEvent event = UserCreatedEvent.builder()
                .eventType("USER_CREATED")
                .userId(user.getId())
                .keycloakId(user.getKeycloakId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();

        log.info("Sending USER_CREATED event for user: {}", user.getId());
        kafkaTemplate.send(userEventsTopic, user.getId().toString(), event);
    }

    public void sendUserUpdatedEvent(User user) {
        UserUpdatedEvent event = UserUpdatedEvent.builder()
                .eventType("USER_UPDATED")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .updatedAt(Instant.now())
                .build();

        log.info("Sending USER_UPDATED event for user: {}", user.getId());
        kafkaTemplate.send(userEventsTopic, user.getId().toString(), event);
    }

    public void sendUserStatusChangedEvent(User user, UserStatus previousStatus, UserStatus newStatus) {
        UserStatusChangedEvent event = UserStatusChangedEvent.builder()
                .eventType("USER_STATUS_CHANGED")
                .userId(user.getId())
                .previousStatus(previousStatus.name())
                .newStatus(newStatus.name())
                .changedAt(Instant.now())
                .build();

        log.info("Sending USER_STATUS_CHANGED event for user: {}", user.getId());
        kafkaTemplate.send(userEventsTopic, user.getId().toString(), event);
    }
}
