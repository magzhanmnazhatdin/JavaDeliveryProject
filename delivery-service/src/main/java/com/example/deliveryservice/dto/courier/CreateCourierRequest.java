package com.example.deliveryservice.dto.courier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new courier")
public class CreateCourierRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Schema(description = "Courier's full name", example = "John Doe")
    private String name;

    @NotBlank(message = "Phone is required")
    @Size(min = 5, max = 50, message = "Phone must be between 5 and 50 characters")
    @Schema(description = "Courier's phone number", example = "+1234567890")
    private String phone;

    @Email(message = "Invalid email format")
    @Schema(description = "Courier's email address", example = "john.doe@example.com")
    private String email;
}
