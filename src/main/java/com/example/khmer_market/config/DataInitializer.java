package com.example.khmer_market.config;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    // Standard valid 1x1 RGBA PNG byte array (RFC 2083 compliant, zero external network downloads)
    private static final byte[] SAMPLE_PNG_BYTES = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41, 0x54,
            0x78, (byte) 0x9C, 0x63, 0x60, 0x60, (byte) 0xF8, 0x0F, 0x00, 0x01, 0x04, 0x01, 0x00, 0x09, (byte) 0x90, 0x02, (byte) 0xBD,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
            (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    private final UserRepository userRepository;
    private final UserSecurityRepository userSecurityRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductPackageRepository productPackageRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public DataInitializer(UserRepository userRepository,
                           UserSecurityRepository userSecurityRepository,
                           FarmerProfileRepository farmerProfileRepository,
                           AddressRepository addressRepository,
                           CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           ProductPackageRepository productPackageRepository,
                           ProductImageRepository productImageRepository,
                           InventoryRepository inventoryRepository,
                           OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           PaymentRepository paymentRepository,
                           ReviewRepository reviewRepository,
                           FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userSecurityRepository = userSecurityRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productPackageRepository = productPackageRepository;
        this.productImageRepository = productImageRepository;
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.reviewRepository = reviewRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if ("test".equalsIgnoreCase(activeProfile)) {
            log.info("Test profile active. Skipping DataInitializer.");
            return;
        }

        // 1. Clean up any legacy static demo users, products, orders, and reviews
        cleanupStaticDemoData();

        // 2. Ensure master agricultural categories exist
        initCategories();

        log.info("Khmer Market startup initialization completed. Zero static users or demo products seeded.");
    }

    /**
     * Purges legacy static demo users, products, sample orders, and reviews from the database.
     */
    private void cleanupStaticDemoData() {
        List<String> demoUsernames = List.of(
                "farmer_sokha", "farmer_vanna", "farmer_dara",
                "buyer_bopha", "buyer_rithy", "buyer_chan",
                "admin"
        );
        List<String> demoOrderNumbers = List.of(
                "KM-20261015-ORD01", "KM-20261015-ORD02",
                "KM-20261015-ORD03", "KM-20261015-ORD04"
        );
        List<String> demoProductNames = List.of(
                "Battambang Phka Rumduol Jasmine Rice",
                "Kampot Black Pepper (GI Certified)",
                "Koh Trung Sweet Pomelo",
                "Takeo Giant Freshwater River Prawns",
                "Mondulkiri Wild Forest Pure Honey",
                "Siem Reap Fresh Organic Lemongrass"
        );

        List<User> demoUsers = demoUsernames.stream()
                .map(userRepository::findByUsername)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        List<Order> demoOrders = orderRepository.findAll().stream()
                .filter(o -> demoOrderNumbers.contains(o.getOrderNumber()) ||
                        (o.getBuyer() != null && demoUsernames.contains(o.getBuyer().getUsername())))
                .toList();

        List<Product> demoProducts = productRepository.findAll().stream()
                .filter(p -> demoProductNames.contains(p.getName()) ||
                        (p.getSeller() != null && demoUsernames.contains(p.getSeller().getUsername())))
                .toList();

        if (demoUsers.isEmpty() && demoOrders.isEmpty() && demoProducts.isEmpty()) {
            return;
        }

        log.info("Purging legacy static demo users, products, orders, and reviews...");

        // A. Delete reviews linked to demo products or demo users
        List<Review> reviewsToDelete = reviewRepository.findAll().stream()
                .filter(r -> (r.getUser() != null && demoUsernames.contains(r.getUser().getUsername())) ||
                        (r.getProduct() != null && (demoProductNames.contains(r.getProduct().getName()) ||
                                (r.getProduct().getSeller() != null && demoUsernames.contains(r.getProduct().getSeller().getUsername())))))
                .toList();
        if (!reviewsToDelete.isEmpty()) {
            reviewRepository.deleteAll(reviewsToDelete);
        }

        // B. Delete demo orders and their associated payments and items
        for (Order o : demoOrders) {
            for (OrderItem item : o.getItems()) {
                reviewRepository.findByOrderItemId(item.getId()).ifPresent(reviewRepository::delete);
            }
            List<Payment> payments = paymentRepository.findByOrderId(o.getId());
            if (!payments.isEmpty()) {
                paymentRepository.deleteAll(payments);
            }
            orderRepository.delete(o);
        }

        // C. Delete demo products and their packages & inventory
        for (Product p : demoProducts) {
            for (ProductPackage pkg : p.getPackages()) {
                inventoryRepository.findByProductPackageId(pkg.getId()).ifPresent(inventoryRepository::delete);
            }
            productRepository.delete(p);
        }

        // D. Delete demo users, farmer profiles, addresses, user securities
        for (User u : demoUsers) {
            farmerProfileRepository.findByUserId(u.getId()).ifPresent(farmerProfileRepository::delete);
            userSecurityRepository.findByUserId(u.getId()).ifPresent(userSecurityRepository::delete);
            List<Address> addresses = addressRepository.findByUserId(u.getId());
            if (!addresses.isEmpty()) {
                addressRepository.deleteAll(addresses);
            }
            userRepository.delete(u);
        }

        log.info("Legacy static demo users and products successfully removed.");
    }

    /**
     * Initializes base marketplace categories if not already present.
     */
    private void initCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        log.info("Initializing marketplace agricultural categories...");
        String catImg1 = fileStorageService.storeBytes(SAMPLE_PNG_BYTES, ".png");
        String catImg2 = fileStorageService.storeBytes(SAMPLE_PNG_BYTES, ".png");
        String catImg3 = fileStorageService.storeBytes(SAMPLE_PNG_BYTES, ".png");
        String catImg4 = fileStorageService.storeBytes(SAMPLE_PNG_BYTES, ".png");
        String catImg5 = fileStorageService.storeBytes(SAMPLE_PNG_BYTES, ".png");
        String catImg6 = fileStorageService.storeBytes(SAMPLE_PNG_BYTES, ".png");

        createCategory("Grains & Rice", "Authentic Cambodian fragrant rice, organic sticky rice, and heritage grains.", catImg1);
        createCategory("Spices & Seasoning", "World-renowned GI Kampot peppers and artisanal spices.", catImg2);
        createCategory("Fresh Tropical Fruits", "Sun-ripened pomelos, mangoes, and specialty tropical fruits.", catImg3);
        createCategory("Fishery & Livestock", "Tonle Sap and Takeo fresh river prawns and artisan meats.", catImg4);
        createCategory("Honey & Forest Harvest", "Wild forest raw honey and natural botanical goods.", catImg5);
        createCategory("Vegetables & Fresh Herbs", "Direct farm fresh herbs and pesticide-free produce.", catImg6);
    }

    private Category createCategory(String name, String description, String imageUrl) {
        Category cat = new Category();
        cat.setName(name);
        cat.setDescription(description);
        cat.setImageUrl(imageUrl);
        return categoryRepository.save(cat);
    }
}
