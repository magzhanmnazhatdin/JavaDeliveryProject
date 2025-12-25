package com.example.userservice.service;

import com.example.userservice.dto.user.*;
import com.example.userservice.entity.UserRole;
import com.example.userservice.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserDto createUser(CreateUserRequest request);

    UserDto getUserById(UUID userId);

    UserDto getUserByKeycloakId(String keycloakId);

    UserDto getCurrentUser(String keycloakId);

    UserDto updateUser(UUID userId, UpdateUserRequest request);

    UserDto updateCurrentUser(String keycloakId, UpdateUserRequest request);

    void updateUserStatus(UUID userId, UserStatus status);

    void deleteUser(UUID userId);

    Page<UserSummaryDto> getAllUsers(Pageable pageable);

    Page<UserSummaryDto> getUsersByRole(UserRole role, Pageable pageable);

    Page<UserSummaryDto> getUsersByStatus(UserStatus status, Pageable pageable);

    Page<UserSummaryDto> searchUsers(String query, Pageable pageable);

    void recordLogin(String keycloakId);

    boolean existsByEmail(String email);

    UserDto getOrCreateUser(String keycloakId, String email, String firstName, String lastName);
}
