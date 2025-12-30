package com.example.deliveryservice.controller;

import com.example.deliveryservice.dto.courier.*;
import com.example.deliveryservice.entity.CourierStatus;
import com.example.deliveryservice.exception.BadRequestException;
import com.example.deliveryservice.exception.ResourceNotFoundException;
import com.example.deliveryservice.service.CourierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/couriers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Courier", description = "Courier management API")
@SecurityRequirement(name = "bearerAuth")
public class CourierController {

    private final CourierService courierService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new courier", description = "Creates a new courier profile")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Courier created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Courier with this phone already exists")
    })
    public ResponseEntity<CourierDto> createCourier(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateCourierRequest request) {
        log.info("REST request to create courier: {}", request.getName());

        // If keycloakId is provided in the request, use it (for API Gateway calls)
        // Otherwise, use the authenticated user's ID
        String keycloakId = request.getKeycloakId();
        if (keycloakId == null || keycloakId.isBlank()) {
            keycloakId = jwt.getSubject();
        }

        CourierDto courier = courierService.createCourierForUser(request, keycloakId);
        return ResponseEntity.status(HttpStatus.CREATED).body(courier);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get courier by ID", description = "Returns courier details by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier found"),
            @ApiResponse(responseCode = "404", description = "Courier not found")
    })
    public ResponseEntity<CourierDto> getCourierById(
            @Parameter(description = "Courier ID") @PathVariable UUID id) {
        log.debug("REST request to get courier: {}", id);
        CourierDto courier = courierService.getCourierById(id);
        return ResponseEntity.ok(courier);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    @Operation(summary = "Get current courier", description = "Returns courier linked to current user")
    public ResponseEntity<CourierDto> getMyCourier(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        try {
            CourierDto courier = courierService.getCourierByKeycloakId(keycloakId);
            return ResponseEntity.ok(courier);
        } catch (ResourceNotFoundException ex) {
            String phone = "dev-" + keycloakId;
            CreateCourierRequest request = CreateCourierRequest.builder()
                    .name(resolveName(jwt))
                    .phone(phone)
                    .email(jwt.getClaimAsString("email"))
                    .build();
            CourierDto created = courierService.createCourierForUser(request, keycloakId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }
    }

    @GetMapping
    @Operation(summary = "Get all couriers", description = "Returns list of all couriers")
    public ResponseEntity<List<CourierDto>> getAllCouriers(
            @Parameter(description = "Filter by status") @RequestParam(required = false) CourierStatus status) {
        log.debug("REST request to get couriers, status filter: {}", status);
        List<CourierDto> couriers;
        if (status != null) {
            couriers = courierService.getCouriersByStatus(status);
        } else {
            couriers = courierService.getAllCouriers();
        }
        return ResponseEntity.ok(couriers);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available couriers", description = "Returns list of available couriers")
    public ResponseEntity<List<CourierDto>> getAvailableCouriers() {
        log.debug("REST request to get available couriers");
        List<CourierDto> couriers = courierService.getAvailableCouriers();
        return ResponseEntity.ok(couriers);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER')")
    @Operation(summary = "Update courier", description = "Updates courier information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courier updated successfully"),
            @ApiResponse(responseCode = "404", description = "Courier not found"),
            @ApiResponse(responseCode = "409", description = "Phone number already in use")
    })
    public ResponseEntity<CourierDto> updateCourier(
            @Parameter(description = "Courier ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateCourierRequest request) {
        log.info("REST request to update courier: {}", id);
        CourierDto courier = courierService.updateCourier(id, request);
        return ResponseEntity.ok(courier);
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    @Operation(summary = "Update current courier", description = "Creates or updates courier for current user")
    public ResponseEntity<CourierDto> upsertMyCourier(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCourierRequest request) {
        String keycloakId = jwt.getSubject();
        try {
            CourierDto existing = courierService.getCourierByKeycloakId(keycloakId);
            CourierDto updated = courierService.updateCourier(existing.getId(), request);
            return ResponseEntity.ok(updated);
        } catch (ResourceNotFoundException ex) {
            if (request.getPhone() == null || request.getPhone().isBlank()) {
                throw new BadRequestException("Phone is required to create courier profile");
            }
            String name = request.getName();
            if (name == null || name.isBlank()) {
                name = resolveName(jwt);
            }
            CreateCourierRequest createRequest = CreateCourierRequest.builder()
                    .name(name)
                    .phone(request.getPhone())
                    .email(request.getEmail() != null ? request.getEmail() : jwt.getClaimAsString("email"))
                    .build();
            CourierDto created = courierService.createCourierForUser(createRequest, keycloakId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER')")
    @Operation(summary = "Update courier status", description = "Updates courier availability status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Courier not found")
    })
    public ResponseEntity<CourierDto> updateCourierStatus(
            @Parameter(description = "Courier ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateCourierStatusRequest request) {
        log.info("REST request to update courier {} status to {}", id, request.getStatus());
        CourierDto courier = courierService.updateCourierStatus(id, request);
        return ResponseEntity.ok(courier);
    }

    @PatchMapping("/me/toggle-availability")
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    @Operation(summary = "Toggle courier availability", description = "Switches between AVAILABLE and OFFLINE")
    public ResponseEntity<CourierDto> toggleAvailability(@AuthenticationPrincipal Jwt jwt) {
        CourierDto courier = courierService.getCourierByKeycloakId(jwt.getSubject());
        if (courier.getStatus() == CourierStatus.BUSY) {
            throw new BadRequestException("Cannot change availability while courier is busy");
        }
        CourierStatus nextStatus =
                courier.getStatus() == CourierStatus.AVAILABLE ? CourierStatus.OFFLINE : CourierStatus.AVAILABLE;
        UpdateCourierStatusRequest request = UpdateCourierStatusRequest.builder()
                .status(nextStatus)
                .build();
        CourierDto updated = courierService.updateCourierStatus(courier.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'COURIER')")
    @Operation(summary = "Update courier location", description = "Updates courier's current GPS location")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Location updated successfully"),
            @ApiResponse(responseCode = "404", description = "Courier not found")
    })
    public ResponseEntity<CourierDto> updateCourierLocation(
            @Parameter(description = "Courier ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateCourierLocationRequest request) {
        log.debug("REST request to update courier {} location", id);
        CourierDto courier = courierService.updateCourierLocation(id, request);
        return ResponseEntity.ok(courier);
    }

    @PatchMapping("/me/location")
    @PreAuthorize("hasAnyRole('COURIER', 'ADMIN')")
    @Operation(summary = "Update current courier location", description = "Updates courier's current GPS location")
    public ResponseEntity<CourierDto> updateMyLocation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateCourierLocationRequest request) {
        CourierDto courier = courierService.getCourierByKeycloakId(jwt.getSubject());
        CourierDto updated = courierService.updateCourierLocation(courier.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete courier", description = "Deletes a courier (Admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Courier deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Cannot delete busy courier"),
            @ApiResponse(responseCode = "404", description = "Courier not found")
    })
    public ResponseEntity<Void> deleteCourier(
            @Parameter(description = "Courier ID") @PathVariable UUID id) {
        log.info("REST request to delete courier: {}", id);
        courierService.deleteCourier(id);
        return ResponseEntity.noContent().build();
    }

    private String resolveName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        if (name != null && !name.isBlank()) {
            return name;
        }
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername;
        }
        return "Courier";
    }
}
