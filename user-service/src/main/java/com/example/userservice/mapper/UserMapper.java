package com.example.userservice.mapper;

import com.example.userservice.dto.user.UserDto;
import com.example.userservice.dto.user.UserSummaryDto;
import com.example.userservice.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    private final AddressMapper addressMapper;
    private final UserPreferencesMapper preferencesMapper;

    public UserMapper(AddressMapper addressMapper, UserPreferencesMapper preferencesMapper) {
        this.addressMapper = addressMapper;
        this.preferencesMapper = preferencesMapper;
    }

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return UserDto.builder()
                .id(user.getId())
                .keycloakId(user.getKeycloakId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .preferredLanguage(user.getPreferredLanguage())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .addresses(user.getAddresses().stream()
                        .map(addressMapper::toDto)
                        .collect(Collectors.toList()))
                .preferences(user.getPreferences() != null ?
                        preferencesMapper.toDto(user.getPreferences()) : null)
                .build();
    }

    public UserSummaryDto toSummaryDto(User user) {
        if (user == null) {
            return null;
        }

        return UserSummaryDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    public List<UserDto> toDtoList(List<User> users) {
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<UserSummaryDto> toSummaryDtoList(List<User> users) {
        return users.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}
