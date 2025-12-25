package com.example.userservice.mapper;

import com.example.userservice.dto.address.AddressDto;
import com.example.userservice.dto.address.CreateAddressRequest;
import com.example.userservice.entity.Address;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AddressMapper {

    public AddressDto toDto(Address address) {
        if (address == null) {
            return null;
        }

        return AddressDto.builder()
                .id(address.getId())
                .label(address.getLabel())
                .streetAddress(address.getStreetAddress())
                .apartment(address.getApartment())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.getIsDefault())
                .deliveryInstructions(address.getDeliveryInstructions())
                .fullAddress(address.getFullAddress())
                .build();
    }

    public Address toEntity(CreateAddressRequest request) {
        if (request == null) {
            return null;
        }

        return Address.builder()
                .label(request.getLabel())
                .streetAddress(request.getStreetAddress())
                .apartment(request.getApartment())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() != null ? request.getCountry() : "Russia")
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .deliveryInstructions(request.getDeliveryInstructions())
                .build();
    }

    public List<AddressDto> toDtoList(List<Address> addresses) {
        return addresses.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
