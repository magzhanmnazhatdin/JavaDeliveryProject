package com.example.userservice.controller;

import com.example.userservice.dto.address.AddressDto;
import com.example.userservice.dto.address.CreateAddressRequest;
import com.example.userservice.dto.address.UpdateAddressRequest;
import com.example.userservice.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Address management API")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @Operation(summary = "Add a new address for current user")
    public ResponseEntity<AddressDto> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateAddressRequest request
    ) {
        String keycloakId = jwt.getSubject();
        AddressDto address = addressService.addAddressForCurrentUser(keycloakId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @GetMapping
    @Operation(summary = "Get current user's addresses")
    public ResponseEntity<List<AddressDto>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        List<AddressDto> addresses = addressService.getCurrentUserAddresses(keycloakId);
        return ResponseEntity.ok(addresses);
    }

    @GetMapping("/{addressId}")
    @Operation(summary = "Get address by ID")
    public ResponseEntity<AddressDto> getAddressById(@PathVariable UUID addressId) {
        AddressDto address = addressService.getAddressById(addressId);
        return ResponseEntity.ok(address);
    }

    @GetMapping("/default")
    @Operation(summary = "Get current user's default address")
    public ResponseEntity<AddressDto> getDefaultAddress(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        AddressDto address = addressService.getDefaultAddressForCurrentUser(keycloakId);
        return ResponseEntity.ok(address);
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an address")
    public ResponseEntity<AddressDto> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId,
            @Valid @RequestBody UpdateAddressRequest request
    ) {
        String keycloakId = jwt.getSubject();
        AddressDto address = addressService.updateAddressForCurrentUser(keycloakId, addressId, request);
        return ResponseEntity.ok(address);
    }

    @PostMapping("/{addressId}/set-default")
    @Operation(summary = "Set address as default")
    public ResponseEntity<Void> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {
        String keycloakId = jwt.getSubject();
        addressService.setDefaultAddressForCurrentUser(keycloakId, addressId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID addressId
    ) {
        String keycloakId = jwt.getSubject();
        addressService.deleteAddressForCurrentUser(keycloakId, addressId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add address for a user (Admin only)")
    public ResponseEntity<AddressDto> addAddressForUser(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateAddressRequest request
    ) {
        AddressDto address = addressService.addAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(address);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get user's addresses (Admin only)")
    public ResponseEntity<List<AddressDto>> getUserAddresses(@PathVariable UUID userId) {
        List<AddressDto> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }
}
