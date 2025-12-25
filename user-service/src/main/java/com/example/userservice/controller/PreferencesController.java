package com.example.userservice.controller;

import com.example.userservice.dto.preferences.UpdatePreferencesRequest;
import com.example.userservice.dto.preferences.UserPreferencesDto;
import com.example.userservice.service.UserPreferencesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
@Tag(name = "Preferences", description = "User preferences management API")
public class PreferencesController {

    private final UserPreferencesService preferencesService;

    @GetMapping
    @Operation(summary = "Get current user's preferences")
    public ResponseEntity<UserPreferencesDto> getMyPreferences(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        UserPreferencesDto preferences = preferencesService.getPreferencesForCurrentUser(keycloakId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping
    @Operation(summary = "Update current user's preferences")
    public ResponseEntity<UserPreferencesDto> updateMyPreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        String keycloakId = jwt.getSubject();
        UserPreferencesDto preferences = preferencesService.updatePreferencesForCurrentUser(keycloakId, request);
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user's preferences (Admin only)")
    public ResponseEntity<UserPreferencesDto> getUserPreferences(@PathVariable UUID userId) {
        UserPreferencesDto preferences = preferencesService.getPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update user's preferences (Admin only)")
    public ResponseEntity<UserPreferencesDto> updateUserPreferences(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        UserPreferencesDto preferences = preferencesService.updatePreferences(userId, request);
        return ResponseEntity.ok(preferences);
    }
}
