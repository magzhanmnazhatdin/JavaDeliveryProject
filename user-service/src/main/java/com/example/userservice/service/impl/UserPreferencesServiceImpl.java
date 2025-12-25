package com.example.userservice.service.impl;

import com.example.userservice.dto.preferences.UpdatePreferencesRequest;
import com.example.userservice.dto.preferences.UserPreferencesDto;
import com.example.userservice.entity.User;
import com.example.userservice.entity.UserPreferences;
import com.example.userservice.exception.PreferencesNotFoundException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.UserPreferencesMapper;
import com.example.userservice.repository.UserPreferencesRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.UserPreferencesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserPreferencesServiceImpl implements UserPreferencesService {

    private final UserPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    private final UserPreferencesMapper preferencesMapper;

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferences(UUID userId) {
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new PreferencesNotFoundException("Preferences not found for user: " + userId));
        return preferencesMapper.toDto(preferences);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferencesForCurrentUser(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return getPreferences(user.getId());
    }

    @Override
    public UserPreferencesDto updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        UserPreferences preferences = preferencesRepository.findByUserId(userId)
                .orElseThrow(() -> new PreferencesNotFoundException("Preferences not found for user: " + userId));

        return updatePreferencesInternal(preferences, request);
    }

    @Override
    public UserPreferencesDto updatePreferencesForCurrentUser(String keycloakId, UpdatePreferencesRequest request) {
        User user = findUserByKeycloakId(keycloakId);
        return updatePreferences(user.getId(), request);
    }

    private UserPreferencesDto updatePreferencesInternal(UserPreferences preferences, UpdatePreferencesRequest request) {
        if (request.getPushNotificationsEnabled() != null) {
            preferences.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        }
        if (request.getEmailNotificationsEnabled() != null) {
            preferences.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        }
        if (request.getSmsNotificationsEnabled() != null) {
            preferences.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        }
        if (request.getOrderUpdatesEnabled() != null) {
            preferences.setOrderUpdatesEnabled(request.getOrderUpdatesEnabled());
        }
        if (request.getPromotionalEmailsEnabled() != null) {
            preferences.setPromotionalEmailsEnabled(request.getPromotionalEmailsEnabled());
        }
        if (request.getDefaultPaymentMethod() != null) {
            preferences.setDefaultPaymentMethod(request.getDefaultPaymentMethod());
        }

        UserPreferences savedPreferences = preferencesRepository.save(preferences);
        log.info("Preferences updated for user {}", preferences.getUser().getId());

        return preferencesMapper.toDto(savedPreferences);
    }

    @Override
    public void createDefaultPreferences(UUID userId) {
        if (preferencesRepository.existsByUserId(userId)) {
            log.debug("Preferences already exist for user {}", userId);
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        UserPreferences preferences = UserPreferences.builder()
                .user(user)
                .build();

        preferencesRepository.save(preferences);
        log.info("Default preferences created for user {}", userId);
    }

    private User findUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found with keycloak ID: " + keycloakId));
    }
}
