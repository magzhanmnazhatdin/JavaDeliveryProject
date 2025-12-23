package com.example.deliveryservice.controller;

import com.example.deliveryservice.dto.courier.CourierDto;
import com.example.deliveryservice.dto.courier.CreateCourierRequest;
import com.example.deliveryservice.entity.CourierStatus;
import com.example.deliveryservice.service.CourierService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CourierController.class)
class CourierControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourierService courierService;

    @Test
    @DisplayName("Should create courier when user is admin")
    @WithMockUser(roles = "ADMIN")
    void createCourier_AsAdmin_Success() throws Exception {
        CreateCourierRequest request = CreateCourierRequest.builder()
                .name("John Doe")
                .phone("+1234567890")
                .email("john@example.com")
                .build();

        CourierDto response = CourierDto.builder()
                .id(UUID.randomUUID())
                .name("John Doe")
                .phone("+1234567890")
                .email("john@example.com")
                .status(CourierStatus.AVAILABLE)
                .build();

        when(courierService.createCourier(any(CreateCourierRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/couriers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Should return 403 when non-admin tries to create courier")
    @WithMockUser(roles = "COURIER")
    void createCourier_AsCourier_Forbidden() throws Exception {
        CreateCourierRequest request = CreateCourierRequest.builder()
                .name("John Doe")
                .phone("+1234567890")
                .build();

        mockMvc.perform(post("/api/couriers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return all couriers")
    @WithMockUser(roles = "ADMIN")
    void getAllCouriers_Success() throws Exception {
        List<CourierDto> couriers = List.of(
                CourierDto.builder()
                        .id(UUID.randomUUID())
                        .name("John Doe")
                        .phone("+1234567890")
                        .status(CourierStatus.AVAILABLE)
                        .build()
        );

        when(courierService.getAllCouriers()).thenReturn(couriers);

        mockMvc.perform(get("/api/couriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    @DisplayName("Should return 400 for invalid request")
    @WithMockUser(roles = "ADMIN")
    void createCourier_InvalidRequest_BadRequest() throws Exception {
        CreateCourierRequest request = CreateCourierRequest.builder()
                .name("") // Invalid: empty name
                .phone("+1234567890")
                .build();

        mockMvc.perform(post("/api/couriers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
