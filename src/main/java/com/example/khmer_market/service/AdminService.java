package com.example.khmer_market.service;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.security.PasswordPolicyValidator;
import com.example.khmer_market.web.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final InventoryRepository inventoryRepository;
    private final AddressRepository addressRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final OrderStateMachine orderStateMachine;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final WalletTransactionRepository walletTransactionRepository;
    private final StoreConfigService storeConfigService;

    public AdminService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        FarmerProfileRepository farmerProfileRepository,
                        CategoryRepository categoryRepository,
                        ProductRepository productRepository,
                        PaymentRepository paymentRepository,
                        ReviewRepository reviewRepository,
                        InventoryRepository inventoryRepository,
                        AddressRepository addressRepository,
                        UserSecurityRepository userSecurityRepository,
                        OrderStateMachine orderStateMachine,
                        FileStorageService fileStorageService,
                        AuditService auditService,
                        PasswordEncoder passwordEncoder,
                        PasswordPolicyValidator passwordPolicyValidator,
                        WalletTransactionRepository walletTransactionRepository,
                        StoreConfigService storeConfigService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.inventoryRepository = inventoryRepository;
        this.addressRepository = addressRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.orderStateMachine = orderStateMachine;
        this.fileStorageService = fileStorageService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.walletTransactionRepository = walletTransactionRepository;
        this.storeConfigService = storeConfigService;
    }

    // --- Dashboard & Analytics ---

    @Transactional(readOnly = true)
    public AdminDashboardStatsDto getDashboardStats() {
        BigDecimal totalPaidGmv = orderRepository.sumTotalPaidRevenue();
        if (totalPaidGmv == null) totalPaidGmv = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal paidSubtotal = orderRepository.sumPaidOrdersSubtotal();
        if (paidSubtotal == null) paidSubtotal = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);

        BigDecimal commissionRate = (storeConfigService != null) ? storeConfigService.getCommissionPercentage() : new BigDecimal("5.00");
        BigDecimal withdrawalFeeRate = (storeConfigService != null) ? storeConfigService.getWithdrawalFee() : new BigDecimal("0.25");

        // Sum completed platform commissions and fees from transaction ledger if available
        BigDecimal ledgerPlatformEarnings = (walletTransactionRepository != null)
                ? walletTransactionRepository.sumAmountByTypeInAndStatus(
                        List.of(WalletTransactionType.COMMISSION, WalletTransactionType.FEE),
                        WalletTransactionStatus.COMPLETED)
                : BigDecimal.ZERO;
        if (ledgerPlatformEarnings == null) ledgerPlatformEarnings = BigDecimal.ZERO;

        // Platform Net Revenue: if ledger has records use ledger, otherwise compute commission rate from paid produce subtotal
        BigDecimal platformNetRevenue;
        if (ledgerPlatformEarnings.compareTo(BigDecimal.ZERO) > 0) {
            platformNetRevenue = ledgerPlatformEarnings.setScale(2, java.math.RoundingMode.HALF_UP);
        } else if (paidSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            platformNetRevenue = paidSubtotal.multiply(commissionRate)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else {
            platformNetRevenue = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // Farmer Disbursed Earnings: produce payouts + seller shipping = Total GMV - platform commission/fees
        BigDecimal farmerDisbursedEarnings = totalPaidGmv.subtract(platformNetRevenue).setScale(2, java.math.RoundingMode.HALF_UP);
        if (farmerDisbursedEarnings.compareTo(BigDecimal.ZERO) < 0) {
            farmerDisbursedEarnings = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }

        long totalOrders = orderRepository.count();
        long activeFarmers = userRepository.countByRoleAndStatus(UserRole.FARMER, UserStatus.ACTIVE);
        long pendingApprovals = farmerProfileRepository.countPendingApprovals();
        long lowStockAlerts = inventoryRepository.countLowStockAlerts();
        long todayOrders = orderRepository.countOrdersSince(LocalDate.now().atStartOfDay());

        return new AdminDashboardStatsDto(
                platformNetRevenue,
                totalPaidGmv,
                farmerDisbursedEarnings,
                commissionRate,
                withdrawalFeeRate,
                totalOrders,
                activeFarmers,
                pendingApprovals,
                lowStockAlerts,
                todayOrders
        );
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsDto getAnalytics() {
        // 1. 30-day daily revenue
        LocalDateTime thirtyDaysAgo = LocalDate.now().minusDays(30).atStartOfDay();
        List<Object[]> dailyRows = orderRepository.findDailyRevenueSince(thirtyDaysAgo);
        Map<String, BigDecimal> dailyMap = new LinkedHashMap<>();
        for (int i = 29; i >= 0; i--) {
            dailyMap.put(LocalDate.now().minusDays(i).toString(), BigDecimal.ZERO);
        }
        for (Object[] row : dailyRows) {
            if (row[0] != null && row[1] != null) {
                dailyMap.put(row[0].toString(), (BigDecimal) row[1]);
            }
        }
        List<String> revLabels = new ArrayList<>(dailyMap.keySet());
        List<BigDecimal> revData = new ArrayList<>(dailyMap.values());

        // 2. Orders by status
        List<Object[]> statusRows = orderRepository.countOrdersByStatus();
        List<String> statusLabels = new ArrayList<>();
        List<Long> statusData = new ArrayList<>();
        for (Object[] row : statusRows) {
            statusLabels.add(row[0].toString().toUpperCase());
            statusData.add((Long) row[1]);
        }

        // 3. Registrations by role
        List<Object[]> roleRows = userRepository.countUsersByRole();
        List<String> roleLabels = new ArrayList<>();
        List<Long> roleData = new ArrayList<>();
        for (Object[] row : roleRows) {
            roleLabels.add(row[0].toString().toUpperCase());
            roleData.add((Long) row[1]);
        }

        // 4. Top categories by sales
        List<Object[]> catRows = orderRepository.findTopCategoriesBySales(PageRequest.of(0, 5));
        List<String> catLabels = new ArrayList<>();
        List<BigDecimal> catData = new ArrayList<>();
        for (Object[] row : catRows) {
            catLabels.add(row[0] != null ? row[0].toString() : "Uncategorized");
            catData.add(row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO);
        }

        // 5. Top farmers by revenue
        List<Object[]> farmerRows = orderRepository.findTopFarmersByRevenue(PageRequest.of(0, 5));
        List<String> farmerLabels = new ArrayList<>();
        List<BigDecimal> farmerData = new ArrayList<>();
        for (Object[] row : farmerRows) {
            farmerLabels.add(row[1] != null ? row[1].toString() : "Farm #" + row[0]);
            farmerData.add(row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO);
        }

        return new AdminAnalyticsDto(
                revLabels, revData,
                statusLabels, statusData,
                roleLabels, roleData,
                catLabels, catData,
                farmerLabels, farmerData
        );
    }

    // --- User Management ---

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String query, UserRole role, UserStatus status, Pageable pageable) {
        String safeQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return userRepository.searchUsers(safeQuery, role, status, pageable);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    @Transactional
    public void updateUserStatus(Long userId, UserStatus newStatus, String adminUsername, String ip) {
        User user = getUser(userId);
        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        userRepository.save(user);

        auditService.logAction(adminUsername, "UPDATE_USER_STATUS", "User", userId, ip,
                Map.of("oldStatus", oldStatus.name(), "newStatus", newStatus.name()));
    }

    @Transactional
    public void unlockUserAccount(Long userId, String adminUsername, String ip) {
        User user = getUser(userId);
        UserSecurity sec = userSecurityRepository.findByUserId(userId).orElse(null);
        if (sec != null) {
            sec.setFailedAttempts(0);
            sec.setLockedUntil(null);
            userSecurityRepository.save(sec);
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        }

        auditService.logAction(adminUsername, "UNLOCK_USER_ACCOUNT", "User", userId, ip,
                Map.of("username", user.getUsername()));
    }

    // --- Farmer Approval Queue ---

    @Transactional(readOnly = true)
    public Page<FarmerProfile> searchFarmerProfiles(String query, String province, UserStatus status, Pageable pageable) {
        String safeQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        String safeProvince = (province != null && !province.isBlank()) ? province.trim() : null;
        return farmerProfileRepository.searchFarmerProfiles(safeQuery, safeProvince, status, pageable);
    }

    @Transactional(readOnly = true)
    public FarmerProfile getFarmerProfile(Long profileId) {
        return farmerProfileRepository.findByIdWithUser(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found: " + profileId));
    }

    @Transactional
    public void approveFarmer(Long profileId, String adminUsername, String ip) {
        FarmerProfile profile = getFarmerProfile(profileId);
        User user = profile.getUser();
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        auditService.logAction(adminUsername, "APPROVE_FARMER", "FarmerProfile", profileId, ip,
                Map.of("farmName", profile.getFarmName(), "userId", user.getId()));
    }

    @Transactional
    public void rejectFarmer(Long profileId, String reason, String adminUsername, String ip) {
        FarmerProfile profile = getFarmerProfile(profileId);
        User user = profile.getUser();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        auditService.logAction(adminUsername, "REJECT_FARMER", "FarmerProfile", profileId, ip,
                Map.of("farmName", profile.getFarmName(), "userId", user.getId(), "reason", reason != null ? reason : "None provided"));
    }

    // --- Category Management ---

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    @Transactional(readOnly = true)
    public long getProductCountForCategory(Long categoryId) {
        return productRepository.countByCategoryId(categoryId);
    }

    @Transactional
    public Category createCategory(CategoryFormDto dto, MultipartFile image, String adminUsername, String ip) {
        if (categoryRepository.existsByName(dto.getName().trim())) {
            throw new IllegalArgumentException("A category with this name already exists");
        }

        Category category = new Category();
        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");

        if (image != null && !image.isEmpty()) {
            String filename = fileStorageService.storeFile(image);
            category.setImageUrl(filename);
        }

        Category saved = categoryRepository.save(category);

        auditService.logAction(adminUsername, "CREATE_CATEGORY", "Category", saved.getId(), ip,
                Map.of("name", saved.getName()));

        return saved;
    }

    @Transactional
    public Category updateCategory(Long id, CategoryFormDto dto, MultipartFile image, String adminUsername, String ip) {
        Category category = getCategory(id);

        if (categoryRepository.existsByNameAndIdNot(dto.getName().trim(), id)) {
            throw new IllegalArgumentException("Another category already exists with this name");
        }

        category.setName(dto.getName().trim());
        category.setDescription(dto.getDescription() != null ? dto.getDescription().trim() : "");

        if (image != null && !image.isEmpty()) {
            if (category.getImageUrl() != null && !category.getImageUrl().isBlank()) {
                fileStorageService.deleteFile(category.getImageUrl());
            }
            String filename = fileStorageService.storeFile(image);
            category.setImageUrl(filename);
        }

        Category saved = categoryRepository.save(category);

        auditService.logAction(adminUsername, "UPDATE_CATEGORY", "Category", saved.getId(), ip,
                Map.of("name", saved.getName()));

        return saved;
    }

    @Transactional
    public void deleteCategory(Long id, String adminUsername, String ip) {
        Category category = getCategory(id);
        long productCount = productRepository.countByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category containing " + productCount + " products. Reassign or remove products first.");
        }

        if (category.getImageUrl() != null && !category.getImageUrl().isBlank()) {
            fileStorageService.deleteFile(category.getImageUrl());
        }

        categoryRepository.delete(category);

        auditService.logAction(adminUsername, "DELETE_CATEGORY", "Category", id, ip,
                Map.of("name", category.getName()));
    }

    // --- Product Moderation ---

    @Transactional(readOnly = true)
    public Page<Product> searchProducts(String query, Long categoryId, Long sellerId, ProductStatus status, Pageable pageable) {
        String safeQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return productRepository.searchAdminProducts(safeQuery, categoryId, sellerId, status, pageable);
    }

    @Transactional(readOnly = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional
    public void toggleProductStatus(Long productId, ProductStatus newStatus, String adminUsername, String ip) {
        Product product = getProduct(productId);
        ProductStatus oldStatus = product.getStatus();
        product.setStatus(newStatus);
        productRepository.save(product);

        auditService.logAction(adminUsername, "TOGGLE_PRODUCT_STATUS", "Product", productId, ip,
                Map.of("name", product.getName(), "oldStatus", oldStatus.name(), "newStatus", newStatus.name()));
    }

    // --- Order Oversight & Override ---

    @Transactional(readOnly = true)
    public Page<Order> searchOrders(String query, OrderStatus status, Pageable pageable) {
        String safeQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return orderRepository.searchAdminOrders(safeQuery, status, pageable);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    @Transactional
    public void overrideOrderStatus(Long orderId, OrderStatus targetStatus, String reason, String adminUsername, String ip) {
        Order order = getOrder(orderId);
        OrderStatus oldStatus = order.getStatus();

        if (targetStatus == OrderStatus.CANCELLED) {
            orderStateMachine.cancelOrder(order, UserRole.ADMIN, reason);
        } else if (targetStatus == OrderStatus.CONFIRMED) {
            orderStateMachine.confirmOrder(order, UserRole.ADMIN);
        } else if (targetStatus == OrderStatus.SHIPPED) {
            orderStateMachine.shipOrder(order, UserRole.ADMIN);
        } else if (targetStatus == OrderStatus.DELIVERED) {
            orderStateMachine.deliverOrder(order, UserRole.ADMIN);
        } else {
            order.setStatus(targetStatus);
            orderRepository.save(order);
        }

        auditService.logAction(adminUsername, "OVERRIDE_ORDER_STATUS", "Order", orderId, ip,
                Map.of("orderNumber", order.getOrderNumber(), "oldStatus", oldStatus.name(), "targetStatus", targetStatus.name(), "reason", reason != null ? reason : ""));
    }

    // --- Payment Logs ---

    @Transactional(readOnly = true)
    public Page<Payment> searchPayments(String query, PaymentMethod method, PaymentStatus status, Pageable pageable) {
        String safeQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return paymentRepository.searchAdminPayments(safeQuery, method, status, pageable);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
    }

    // --- Review Moderation ---

    @Transactional(readOnly = true)
    public Page<Review> searchReviews(String query, Integer rating, ReviewStatus status, Pageable pageable) {
        String safeQuery = (query != null && !query.isBlank()) ? query.trim() : null;
        return reviewRepository.searchAdminReviews(safeQuery, rating, status, pageable);
    }

    @Transactional(readOnly = true)
    public Review getReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
    }

    @Transactional
    public void updateReviewStatus(Long reviewId, ReviewStatus status, String adminUsername, String ip) {
        Review review = getReview(reviewId);
        ReviewStatus oldStatus = review.getStatus();
        review.setStatus(status);
        reviewRepository.save(review);

        auditService.logAction(adminUsername, "MODERATE_REVIEW_STATUS", "Review", reviewId, ip,
                Map.of("oldStatus", oldStatus.name(), "newStatus", status.name(), "productId", review.getProduct().getId()));
    }

    @Transactional
    public void deleteReview(Long reviewId, String adminUsername, String ip) {
        Review review = getReview(reviewId);
        reviewRepository.delete(review);

        auditService.logAction(adminUsername, "DELETE_REVIEW", "Review", reviewId, ip,
                Map.of("productId", review.getProduct().getId(), "author", review.getUser().getUsername()));
    }

    // --- Admin Settings & Password ---

    @Transactional
    public void changeAdminPassword(String adminUsername, ChangePasswordDto dto, String ip) {
        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        if (!dto.isPasswordsMatch()) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        passwordPolicyValidator.validatePassword(dto.getNewPassword());

        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(admin);

        auditService.logAction(adminUsername, "CHANGE_ADMIN_PASSWORD", "User", admin.getId(), ip,
                Map.of("username", adminUsername));
    }
}
