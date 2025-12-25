package com.example.userservice.service;

import com.example.userservice.dto.preferences.UpdatePreferencesRequest;
import com.example.userservice.dto.preferences.UserPreferencesDto;

import java.util.UUID;

public interface UserPreferencesService {

    UserPreferencesDto getPreferences(UUID userId);

    UserPreferencesDto getPreferencesForCurrentUser(String keycloakId);

    UserPreferencesDto updatePreferences(UUID userId, UpdatePreferencesRequest request);

    UserPreferencesDto updatePreferencesForCurrentUser(String keycloakId, UpdatePreferencesRequest request);

    void createDefaultPreferences(UUID userId);

    void resetPreferences(UUID userId);

    void resetPreferencesForCurrentUser(String keycloakId);
}
