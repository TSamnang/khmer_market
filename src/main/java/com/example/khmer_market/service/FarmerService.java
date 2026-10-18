package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.FarmerProfile;
import com.example.khmer_market.domain.entity.Inventory;
import com.example.khmer_market.domain.entity.User;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.web.dto.FarmerProfileDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FarmerService {

    private final FarmerProfileRepository farmerProfileRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final FileStorageService fileStorageService;

    public FarmerService(FarmerProfileRepository farmerProfileRepository,
                         UserRepository userRepository,
                         ProductRepository productRepository,
                         OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         InventoryRepository inventoryRepository,
                         FileStorageService fileStorageService) {
        this.farmerProfileRepository = farmerProfileRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public FarmerProfile getProfile(Long userId) {
        return farmerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for user: " + userId));
    }

    @Transactional
    public FarmerProfile updateProfile(Long userId, FarmerProfileDto dto, MultipartFile farmImageFile) {
        FarmerProfile profile = farmerProfileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
            FarmerProfile p = new FarmerProfile();
            p.setUser(user);
            return p;
        });

        profile.setFarmName(dto.getFarmName().trim());
        profile.setFarmerName(dto.getFarmerName().trim());
        profile.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        profile.setAddress(dto.getAddress() != null ? dto.getAddress().trim() : "");
        profile.setProvince(dto.getProvince().trim());
        profile.setFarmDescription(dto.getFarmDescription());

        if (farmImageFile != null && !farmImageFile.isEmpty()) {
            if (profile.getFarmImage() != null) {
                fileStorageService.deleteFile(profile.getFarmImage());
            }
            String filename = fileStorageService.storeFile(farmImageFile);
            profile.setFarmImage(filename);
        }

        return farmerProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(Long sellerId) {
        Map<String, Object> stats = new HashMap<>();

        long totalProducts = productRepository.countBySellerId(sellerId);
        long pendingOrders = orderRepository.findBySellerId(sellerId, OrderStatus.PENDING, PageRequest.of(0, 1)).getTotalElements();
        long confirmedOrders = orderRepository.findBySellerId(sellerId, OrderStatus.CONFIRMED, PageRequest.of(0, 1)).getTotalElements();

        List<Inventory> lowStock = inventoryRepository.findLowStockInventoriesForSeller(sellerId);

        stats.put("totalProducts", totalProducts);
        stats.put("pendingOrders", pendingOrders);
        stats.put("confirmedOrders", confirmedOrders);
        stats.put("ordersToFulfil", pendingOrders + confirmedOrders);
        stats.put("lowStockCount", lowStock.size());
        stats.put("lowStockItems", lowStock);

        return stats;
    }
}
