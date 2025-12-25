package com.example.userservice.mapper;

import com.example.userservice.dto.preferences.UserPreferencesDto;
import com.example.userservice.entity.UserPreferences;
import org.springframework.stereotype.Component;

@Component
public class UserPreferencesMapper {

    public UserPreferencesDto toDto(UserPreferences preferences) {
        if (preferences == null) {
            return null;
        }

        return UserPreferencesDto.builder()
                .id(preferences.getId())
                .pushNotificationsEnabled(preferences.getPushNotificationsEnabled())
                .emailNotificationsEnabled(preferences.getEmailNotificationsEnabled())
                .smsNotificationsEnabled(preferences.getSmsNotificationsEnabled())
                .orderUpdatesEnabled(preferences.getOrderUpdatesEnabled())
                .promotionalEmailsEnabled(preferences.getPromotionalEmailsEnabled())
                .defaultPaymentMethod(preferences.getDefaultPaymentMethod())
                .build();
    }
}
