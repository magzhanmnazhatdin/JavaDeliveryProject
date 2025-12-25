package com.example.userservice.controller;

import com.example.userservice.config.SecurityConfig;
import com.example.userservice.dto.user.*;
import com.example.userservice.entity.UserRole;
import com.example.userservice.entity.UserStatus;
import com.example.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("Should get current user profile")
    void getCurrentUser_Success() throws Exception {
        String keycloakId = UUID.randomUUID().toString();
        UUID userId = UUID.randomUUID();

        UserDto userDto = UserDto.builder()
                .id(userId)
                .keycloakId(keycloakId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getOrCreateUser(eq(keycloakId), any(), any(), any())).thenReturn(userDto);

        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(keycloakId)
                                .claim("email", "john@example.com")
                                .claim("given_name", "John")
                                .claim("family_name", "Doe")
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Should return 401 when not authenticated")
    void getCurrentUser_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should update current user profile")
    void updateCurrentUser_Success() throws Exception {
        String keycloakId = UUID.randomUUID().toString();

        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .phone("+9876543210")
                .build();

        UserDto updatedDto = UserDto.builder()
                .keycloakId(keycloakId)
                .firstName("Jane")
                .phone("+9876543210")
                .build();

        when(userService.updateCurrentUser(eq(keycloakId), any(UpdateUserRequest.class))).thenReturn(updatedDto);

        mockMvc.perform(put("/api/users/me")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(keycloakId)
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.phone").value("+9876543210"));
    }

    @Test
    @DisplayName("Should get all users when admin")
    void getAllUsers_AsAdmin_Success() throws Exception {
        UserSummaryDto summaryDto = UserSummaryDto.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .fullName("John Doe")
                .role(UserRole.CUSTOMER)
                .build();

        Page<UserSummaryDto> page = new PageImpl<>(List.of(summaryDto));

        when(userService.getAllUsers(any())).thenReturn(page);

        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("john@example.com"));
    }

    @Test
    @DisplayName("Should return 403 when non-admin tries to get all users")
    void getAllUsers_AsCustomer_Forbidden() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("CUSTOMER"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should get user by ID when admin")
    void getUserById_AsAdmin_Success() throws Exception {
        UUID userId = UUID.randomUUID();

        UserDto userDto = UserDto.builder()
                .id(userId)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getUserById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/users/{userId}", userId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    @DisplayName("Should update user status when admin")
    void updateUserStatus_AsAdmin_Success() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(patch("/api/users/{userId}/status", userId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf())
                        .param("status", "SUSPENDED"))
                .andExpect(status().isOk());

        verify(userService).updateUserStatus(userId, UserStatus.SUSPENDED);
    }

    @Test
    @DisplayName("Should search users when admin")
    void searchUsers_AsAdmin_Success() throws Exception {
        UserSummaryDto summaryDto = UserSummaryDto.builder()
                .id(UUID.randomUUID())
                .email("john@example.com")
                .fullName("John Doe")
                .build();

        Page<UserSummaryDto> page = new PageImpl<>(List.of(summaryDto));

        when(userService.searchUsers(eq("john"), any())).thenReturn(page);

        mockMvc.perform(get("/api/users/search")
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("query", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("John Doe"));
    }

    @Test
    @DisplayName("Should delete user when admin")
    void deleteUser_AsAdmin_Success() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(delete("/api/users/{userId}", userId)
                        .with(jwt().jwt(jwt -> jwt
                                .subject(UUID.randomUUID().toString())
                                .claim("realm_access", Map.of("roles", List.of("ADMIN"))))
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(userId);
    }
}
