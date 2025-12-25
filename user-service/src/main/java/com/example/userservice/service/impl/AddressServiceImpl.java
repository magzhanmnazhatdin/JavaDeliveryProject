package com.example.userservice.service.impl;

import com.example.userservice.dto.address.AddressDto;
import com.example.userservice.dto.address.CreateAddressRequest;
import com.example.userservice.dto.address.UpdateAddressRequest;
import com.example.userservice.entity.Address;
import com.example.userservice.entity.User;
import com.example.userservice.exception.AddressNotFoundException;
import com.example.userservice.exception.UserNotFoundException;
import com.example.userservice.mapper.AddressMapper;
import com.example.userservice.repository.AddressRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    public AddressDto addAddress(UUID userId, CreateAddressRequest request) {
        User user = findUserById(userId);
        return addAddressInternal(user, request);
    }

    @Override
    public AddressDto addAddressForCurrentUser(String keycloakId, CreateAddressRequest request) {
        User user = findUserByKeycloakId(keycloakId);
        return addAddressInternal(user, request);
    }

    private AddressDto addAddressInternal(User user, CreateAddressRequest request) {
        Address address = addressMapper.toEntity(request);
        address.setUser(user);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultForUser(user.getId(), UUID.randomUUID());
            address.setIsDefault(true);
        } else if (addressRepository.countByUserId(user.getId()) == 0) {
            address.setIsDefault(true);
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address {} added for user {}", savedAddress.getId(), user.getId());

        return addressMapper.toDto(savedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDto getAddressById(UUID addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found: " + addressId));
        return addressMapper.toDto(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getUserAddresses(UUID userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        return addressMapper.toDtoList(addresses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getCurrentUserAddresses(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return getUserAddresses(user.getId());
    }

    @Override
    public AddressDto updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found: " + addressId));
        return updateAddressInternal(address, request);
    }

    @Override
    public AddressDto updateAddressForCurrentUser(String keycloakId, UUID addressId, UpdateAddressRequest request) {
        User user = findUserByKeycloakId(keycloakId);
        return updateAddress(user.getId(), addressId, request);
    }

    private AddressDto updateAddressInternal(Address address, UpdateAddressRequest request) {
        if (request.getLabel() != null) {
            address.setLabel(request.getLabel());
        }
        if (request.getStreetAddress() != null) {
            address.setStreetAddress(request.getStreetAddress());
        }
        if (request.getApartment() != null) {
            address.setApartment(request.getApartment());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getLatitude() != null) {
            address.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            address.setLongitude(request.getLongitude());
        }
        if (request.getDeliveryInstructions() != null) {
            address.setDeliveryInstructions(request.getDeliveryInstructions());
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address {} updated", savedAddress.getId());

        return addressMapper.toDto(savedAddress);
    }

    @Override
    public void setDefaultAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found: " + addressId));

        addressRepository.clearDefaultForUser(userId, addressId);
        address.setIsDefault(true);
        addressRepository.save(address);

        log.info("Address {} set as default for user {}", addressId, userId);
    }

    @Override
    public void setDefaultAddressForCurrentUser(String keycloakId, UUID addressId) {
        User user = findUserByKeycloakId(keycloakId);
        setDefaultAddress(user.getId(), addressId);
    }

    @Override
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AddressNotFoundException("Address not found: " + addressId));

        boolean wasDefault = address.getIsDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findByUserId(userId).stream()
                    .findFirst()
                    .ifPresent(a -> {
                        a.setIsDefault(true);
                        addressRepository.save(a);
                    });
        }

        log.info("Address {} deleted for user {}", addressId, userId);
    }

    @Override
    public void deleteAddressForCurrentUser(String keycloakId, UUID addressId) {
        User user = findUserByKeycloakId(keycloakId);
        deleteAddress(user.getId(), addressId);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDto getDefaultAddress(UUID userId) {
        Address address = addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseThrow(() -> new AddressNotFoundException("No default address found for user: " + userId));
        return addressMapper.toDto(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressDto getDefaultAddressForCurrentUser(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return getDefaultAddress(user.getId());
    }

    private User findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private User findUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException("User not found with keycloak ID: " + keycloakId));
    }
}
