package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.Address;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.AddressRepository;
import com.example.khmer_market.repository.UserRepository;
import com.example.khmer_market.web.dto.AddressDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository,
                          UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<Address> getUserAddresses(Long userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Address getAddress(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found or access denied"));
    }

    @Transactional
    public Address createAddress(Long userId, AddressDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Address address = new Address();
        address.setUser(user);
        address.setName(dto.getName().trim());
        address.setPhone(dto.getPhone().trim());
        address.setProvince(dto.getProvince().trim());
        address.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");
        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(Long addressId, Long userId, AddressDto dto) {
        Address existing = getAddress(addressId, userId);

        long usageCount = addressRepository.countOrdersUsingAddress(addressId);
        if (usageCount > 0) {
            // Copy-on-write pattern: past orders must retain historic address snapshot
            Address newAddress = new Address();
            newAddress.setUser(existing.getUser());
            newAddress.setName(dto.getName().trim());
            newAddress.setPhone(dto.getPhone().trim());
            newAddress.setProvince(dto.getProvince().trim());
            newAddress.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");
            return addressRepository.save(newAddress);
        } else {
            existing.setName(dto.getName().trim());
            existing.setPhone(dto.getPhone().trim());
            existing.setProvince(dto.getProvince().trim());
            existing.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");
            return addressRepository.save(existing);
        }
    }

    @Transactional
    public void deleteAddress(Long addressId, Long userId) {
        Address address = getAddress(addressId, userId);
        long usageCount = addressRepository.countOrdersUsingAddress(addressId);
        if (usageCount > 0) {
            throw new IllegalArgumentException("This address is associated with previous orders and cannot be removed. You may add a new address instead.");
        }
        addressRepository.delete(address);
    }
}
