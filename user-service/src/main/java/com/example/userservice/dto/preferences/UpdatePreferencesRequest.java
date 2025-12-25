package com.example.userservice.dto.preferences;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferencesRequest {

    private Boolean pushNotificationsEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean orderUpdatesEnabled;
    private Boolean promotionalEmailsEnabled;

    @Size(max = 50, message = "Payment method must not exceed 50 characters")
    private String defaultPaymentMethod;
}
