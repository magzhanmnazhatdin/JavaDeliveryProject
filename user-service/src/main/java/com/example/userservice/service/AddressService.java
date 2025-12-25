package com.example.userservice.service;

import com.example.userservice.dto.address.AddressDto;
import com.example.userservice.dto.address.CreateAddressRequest;
import com.example.userservice.dto.address.UpdateAddressRequest;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    AddressDto addAddress(UUID userId, CreateAddressRequest request);

    AddressDto addAddressForCurrentUser(String keycloakId, CreateAddressRequest request);

    AddressDto getAddressById(UUID addressId);

    List<AddressDto> getUserAddresses(UUID userId);

    List<AddressDto> getCurrentUserAddresses(String keycloakId);

    AddressDto updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request);

    AddressDto updateAddressForCurrentUser(String keycloakId, UUID addressId, UpdateAddressRequest request);

    void setDefaultAddress(UUID userId, UUID addressId);

    void setDefaultAddressForCurrentUser(String keycloakId, UUID addressId);

    void deleteAddress(UUID userId, UUID addressId);

    void deleteAddressForCurrentUser(String keycloakId, UUID addressId);

    AddressDto getDefaultAddress(UUID userId);

    AddressDto getDefaultAddressForCurrentUser(String keycloakId);
}
