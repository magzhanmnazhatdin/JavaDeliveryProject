package com.example.userservice.dto.user;

import com.example.userservice.dto.address.AddressDto;
import com.example.userservice.dto.preferences.UserPreferencesDto;
import com.example.userservice.entity.UserRole;
import com.example.userservice.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String keycloakId;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private Boolean emailVerified;
    private Boolean phoneVerified;
    private String preferredLanguage;
    private Instant createdAt;
    private Instant lastLoginAt;
    private List<AddressDto> addresses;
    private UserPreferencesDto preferences;
}
