package com.example.userservice.service.impl;

import com.example.userservice.dto.user.*;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserPreferences;
import com.example.userservice.entity.UserRole;
import com.example.userservice.entity.UserStatus;
import com.example.userservice.exception.UserAlreadyExistsException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.kafka.UserEventProducer;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserEventProducer userEventProducer;

    @Override
    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email already exists: " + request.getEmail());
        }

        if (userRepository.existsByKeycloakId(request.getKeycloakId())) {
            throw new UserAlreadyExistsException("User with keycloak ID already exists: " + request.getKeycloakId());
        }

        User user = User.builder()
                .keycloakId(request.getKeycloakId())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(request.getRole() != null ? request.getRole() : UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        UserPreferences preferences = UserPreferences.builder()
                .user(user)
                .build();
        user.setPreferences(preferences);

        User savedUser = userRepository.save(user);
        log.info("User created with ID: {}", savedUser.getId());

        userEventProducer.sendUserCreatedEvent(savedUser);

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        User user = findUserById(userId);
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakIdWithDetails(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found with keycloak ID: " + keycloakId));
        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String keycloakId) {
        return getUserByKeycloakId(keycloakId);
    }

    @Override
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        User user = findUserById(userId);
        return updateUserInternal(user, request);
    }

    @Override
    public UserDto updateCurrentUser(String keycloakId, UpdateUserRequest request) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found with keycloak ID: " + keycloakId));
        return updateUserInternal(user, request);
    }

    private UserDto updateUserInternal(User user, UpdateUserRequest request) {
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPreferredLanguage() != null) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }

        User savedUser = userRepository.save(user);
        log.info("User {} updated", savedUser.getId());

        userEventProducer.sendUserUpdatedEvent(savedUser);

        return userMapper.toDto(savedUser);
    }

    @Override
    public void updateUserStatus(UUID userId, UserStatus status) {
        User user = findUserById(userId);
        UserStatus previousStatus = user.getStatus();
        user.setStatus(status);
        userRepository.save(user);

        log.info("User {} status changed from {} to {}", userId, previousStatus, status);
        userEventProducer.sendUserStatusChangedEvent(user, previousStatus, status);
    }

    @Override
    public void deleteUser(UUID userId) {
        User user = findUserById(userId);
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        log.info("User {} marked as deleted", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDto> getUsersByRole(UserRole role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(userMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDto> getUsersByStatus(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable)
                .map(userMapper::toSummaryDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryDto> searchUsers(String query, Pageable pageable) {
        return userRepository.searchUsers(query, pageable)
                .map(userMapper::toSummaryDto);
    }

    @Override
    public void recordLogin(String keycloakId) {
        userRepository.findByKeycloakId(keycloakId).ifPresent(user -> {
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            log.debug("Recorded login for user: {}", user.getId());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserDto getOrCreateUser(String keycloakId, String email, String firstName, String lastName) {
        return userRepository.findByKeycloakIdWithDetails(keycloakId)
                .map(userMapper::toDto)
                .orElseGet(() -> {
                    CreateUserRequest request = CreateUserRequest.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .build();
                    return createUser(request);
                });
    }

    private User findUserById(UUID userId) {
        return userRepository.findByIdWithAddressesAndPreferences(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }
}
