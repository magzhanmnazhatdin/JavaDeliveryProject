package com.example.userservice.service;

import com.example.userservice.dto.user.*;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserRole;
import com.example.userservice.entity.UserStatus;
import com.example.userservice.exception.UserAlreadyExistsException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.kafka.UserEventProducer;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEventProducer userEventProducer;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserDto userDto;
    private UUID userId;
    private String keycloakId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        keycloakId = UUID.randomUUID().toString();

        user = User.builder()
                .id(userId)
                .keycloakId(keycloakId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();

        userDto = UserDto.builder()
                .id(userId)
                .keycloakId(keycloakId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Should create user successfully")
    void createUser_Success() {
        CreateUserRequest request = CreateUserRequest.builder()
                .keycloakId(keycloakId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByKeycloakId(request.getKeycloakId())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.createUser(request);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(userEventProducer).sendUserCreatedEvent(any(User.class));
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email exists")
    void createUser_EmailExists_ThrowsException() {
        CreateUserRequest request = CreateUserRequest.builder()
                .keycloakId(keycloakId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_Success() {
        when(userRepository.findByIdWithAddressesAndPreferences(userId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException when user not found")
    void getUserById_NotFound() {
        when(userRepository.findByIdWithAddressesAndPreferences(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should get user by keycloak ID successfully")
    void getUserByKeycloakId_Success() {
        when(userRepository.findByKeycloakIdWithDetails(keycloakId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.getUserByKeycloakId(keycloakId);

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(keycloakId);
    }

    @Test
    @DisplayName("Should update user successfully")
    void updateUser_Success() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .phone("+9876543210")
                .build();

        UserDto updatedDto = UserDto.builder()
                .id(userId)
                .firstName("Jane")
                .phone("+9876543210")
                .build();

        when(userRepository.findByIdWithAddressesAndPreferences(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(updatedDto);

        UserDto result = userService.updateUser(userId, request);

        assertThat(result.getFirstName()).isEqualTo("Jane");
        verify(userEventProducer).sendUserUpdatedEvent(any(User.class));
    }

    @Test
    @DisplayName("Should update user status")
    void updateUserStatus_Success() {
        when(userRepository.findByIdWithAddressesAndPreferences(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.updateUserStatus(userId, UserStatus.SUSPENDED);

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getStatus() == UserStatus.SUSPENDED
        ));
        verify(userEventProducer).sendUserStatusChangedEvent(any(User.class), eq(UserStatus.ACTIVE), eq(UserStatus.SUSPENDED));
    }

    @Test
    @DisplayName("Should delete user (soft delete)")
    void deleteUser_Success() {
        when(userRepository.findByIdWithAddressesAndPreferences(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.deleteUser(userId);

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getStatus() == UserStatus.DELETED
        ));
    }

    @Test
    @DisplayName("Should get all users with pagination")
    void getAllUsers_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));
        UserSummaryDto summaryDto = UserSummaryDto.builder()
                .id(userId)
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toSummaryDto(user)).thenReturn(summaryDto);

        Page<UserSummaryDto> result = userService.getAllUsers(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("Should get users by role")
    void getUsersByRole_Success() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));
        UserSummaryDto summaryDto = UserSummaryDto.builder()
                .id(userId)
                .role(UserRole.CUSTOMER)
                .build();

        when(userRepository.findByRole(UserRole.CUSTOMER, pageable)).thenReturn(userPage);
        when(userMapper.toSummaryDto(user)).thenReturn(summaryDto);

        Page<UserSummaryDto> result = userService.getUsersByRole(UserRole.CUSTOMER, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRole(UserRole.CUSTOMER, pageable);
    }

    @Test
    @DisplayName("Should check if email exists")
    void existsByEmail_Success() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        boolean result = userService.existsByEmail("john@example.com");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should get or create user - existing user")
    void getOrCreateUser_ExistingUser() {
        when(userRepository.findByKeycloakIdWithDetails(keycloakId)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = userService.getOrCreateUser(keycloakId, "john@example.com", "John", "Doe");

        assertThat(result).isNotNull();
        assertThat(result.getKeycloakId()).isEqualTo(keycloakId);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should record login timestamp")
    void recordLogin_Success() {
        when(userRepository.findByKeycloakId(keycloakId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.recordLogin(keycloakId);

        verify(userRepository).save(argThat(savedUser ->
                savedUser.getLastLoginAt() != null
        ));
    }
}
