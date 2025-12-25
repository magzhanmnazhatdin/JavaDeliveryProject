package com.example.userservice.dto.preferences;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesDto {

    private UUID id;
    private Boolean pushNotificationsEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean orderUpdatesEnabled;
    private Boolean promotionalEmailsEnabled;
    private String defaultPaymentMethod;
}
