package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.*;
import com.example.khmer_market.web.dto.AdminDashboardStatsDto;
import com.example.khmer_market.web.dto.CheckoutDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlatformRevenueAndMonetizationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private StoreConfigService storeConfigService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPackageRepository productPackageRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private User testAdmin;
    private User testSeller;
    private User testBuyer;
    private Address testAddress;
    private ProductPackage testPackage;

    @BeforeEach
    void setUp() {
        // Create Admin User for platform fee crediting
        testAdmin = userRepository.findByUsername("admin").orElseGet(() -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@khmermarket.gov.kh");
            admin.setPassword("AdminSecurePass123!");
            admin.setFullName("Platform Owner");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            return userRepository.saveAndFlush(admin);
        });

        // Create Farmer
        testSeller = new User();
        testSeller.setUsername("farmer_chhun_" + System.nanoTime());
        testSeller.setEmail("farmer_chhun_" + System.nanoTime() + "@market.kh");
        testSeller.setPassword("SecurePass123!");
        testSeller.setFullName("Chhun Farmer");
        testSeller.setRole(UserRole.FARMER);
        testSeller.setStatus(UserStatus.ACTIVE);
        testSeller.setBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        testSeller = userRepository.saveAndFlush(testSeller);

        FarmerProfile profile = new FarmerProfile();
        profile.setUser(testSeller);
        profile.setFarmName("Battambang Green Valley");
        profile.setFarmerName("Chhun Sok");
        profile.setProvince("Battambang");
        farmerProfileRepository.saveAndFlush(profile);

        // Create Buyer
        testBuyer = new User();
        testBuyer.setUsername("buyer_vireak_" + System.nanoTime());
        testBuyer.setEmail("buyer_vireak_" + System.nanoTime() + "@market.kh");
        testBuyer.setPassword("SecurePass123!");
        testBuyer.setFullName("Vireak Buyer");
        testBuyer.setRole(UserRole.BUYER);
        testBuyer.setStatus(UserStatus.ACTIVE);
        testBuyer.setBalance(new BigDecimal("100.00"));
        testBuyer = userRepository.saveAndFlush(testBuyer);

        testAddress = new Address();
        testAddress.setUser(testBuyer);
        testAddress.setName("Vireak Buyer");
        testAddress.setPhone("012345678");
        testAddress.setAddress("Street 271");
        testAddress.setProvince("Phnom Penh");
        testAddress = addressRepository.saveAndFlush(testAddress);

        Product product = new Product();
        product.setSeller(testSeller);
        product.setName("Battambang Premium Rice " + System.nanoTime());
        product.setDescription("Organic Jasmine Rice");
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.saveAndFlush(product);

        testPackage = new ProductPackage();
        testPackage.setProduct(product);
        testPackage.setPackageName("5 kg Bag");
        testPackage.setPrice(new BigDecimal("10.00"));
        testPackage = productPackageRepository.saveAndFlush(testPackage);

        Inventory inv = new Inventory(testPackage, 100, 0);
        inventoryRepository.saveAndFlush(inv);
    }

    @Test
    @DisplayName("Seller shipping and platform commission distribution math is exact")
    void testCommissionAndSellerShippingDistribution() {
        BigDecimal adminInitialBalance = walletService.getBalance(testAdmin.getId());
        BigDecimal sellerInitialBalance = walletService.getBalance(testSeller.getId());

        // Produce subtotal: $20.00, Shipping: $1.50 -> Buyer pays $21.50
        BigDecimal subtotal = new BigDecimal("20.00");
        BigDecimal shippingFee = new BigDecimal("1.50");
        String orderNumber = "KM-TEST-COMM-01";

        WalletTransaction sellerTx = walletService.distributeOrderEarnings(testSeller.getId(), subtotal, shippingFee, orderNumber);
        assertNotNull(sellerTx);
        assertEquals(WalletTransactionType.EARNING, sellerTx.getType());

        // Commission = 5% of $20.00 = $1.00
        // Seller earnings = ($20.00 - $1.00) + $1.50 = $20.50
        BigDecimal expectedSellerEarnings = new BigDecimal("20.50");
        assertEquals(expectedSellerEarnings, sellerTx.getAmount());

        BigDecimal sellerNewBalance = walletService.getBalance(testSeller.getId());
        assertEquals(sellerInitialBalance.add(expectedSellerEarnings), sellerNewBalance);

        // Platform Admin receives $1.00 commission
        BigDecimal adminNewBalance = walletService.getBalance(testAdmin.getId());
        assertEquals(adminInitialBalance.add(new BigDecimal("1.00")), adminNewBalance);

        // Financial conservation: Seller Earnings ($20.50) + Platform Commission ($1.00) == Total ($21.50)
        assertEquals(subtotal.add(shippingFee), sellerTx.getAmount().add(new BigDecimal("1.00")));
    }

    @Test
    @DisplayName("Withdrawal charges processing fee and credits platform admin wallet")
    void testWithdrawalProcessingFeeCreditedToPlatform() {
        BigDecimal adminInitialBalance = walletService.getBalance(testAdmin.getId());

        // Buyer has $100.00, withdraws $50.00
        BigDecimal withdrawAmt = new BigDecimal("50.00");
        BigDecimal fee = storeConfigService.getWithdrawalFee(); // $0.25

        WalletTransaction wdTx = walletService.withdraw(testBuyer.getId(), withdrawAmt, "ABA Bank", "012888999", "VIREAK");
        assertNotNull(wdTx);
        assertEquals(new BigDecimal("50.00"), wdTx.getAmount());
        assertEquals(new BigDecimal("50.00"), walletService.getBalance(testBuyer.getId()));

        // Admin received $0.25 payout fee
        BigDecimal adminNewBalance = walletService.getBalance(testAdmin.getId());
        assertEquals(adminInitialBalance.add(fee), adminNewBalance);
    }

    @Test
    @DisplayName("Admin dashboard stats clearly separates Platform Net Revenue from Marketplace GMV")
    void testAdminDashboardStatsSeparatesRevenueAndGmv() {
        // Buyer puts 2 packages ($20.00) in cart and checks out with Balance
        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 2);
        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(testAddress.getId());
        dto.setPaymentMethod("balance");
        dto.setCheckoutToken(token);

        List<Order> orders = checkoutService.processCheckout(testBuyer.getId(), dto, session);
        assertEquals(1, orders.size());
        Order order = orders.get(0);

        AdminDashboardStatsDto stats = adminService.getDashboardStats();
        assertNotNull(stats);

        BigDecimal expectedGmv = order.getTotalAmount();
        BigDecimal expectedSellerPayout = order.getSubtotal().subtract(new BigDecimal("1.00")).add(order.getShippingFee());

        // Total Buyer Payment (Marketplace GMV) matches order total
        assertEquals(expectedGmv, stats.marketplaceGmv());

        // Platform Owner Net Revenue is ONLY $1.00 (5% commission on $20 produce), NOT the full GMV
        assertEquals(new BigDecimal("1.00"), stats.platformNetRevenue());

        // Farmer Disbursed Earnings is ($19 produce + seller shipping fee)
        assertEquals(expectedSellerPayout, stats.farmerDisbursedEarnings());

        // Conservation equation: Platform Revenue + Farmer Earnings == Marketplace GMV
        assertEquals(stats.marketplaceGmv(), stats.platformNetRevenue().add(stats.farmerDisbursedEarnings()));
    }

    @Test
    @DisplayName("Card payment immediately disburses produce and shipping to seller and commission to platform")
    void testCardPaymentDisbursesSellerAndPlatformEarnings() {
        BigDecimal adminInitial = walletService.getBalance(testAdmin.getId());
        BigDecimal sellerInitial = walletService.getBalance(testSeller.getId());

        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 2);
        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(testAddress.getId());
        dto.setPaymentMethod("card");
        dto.setCheckoutToken(token);
        dto.setCardNumber("4242 4242 4242 4242");
        dto.setCardExpiry("12/28");
        dto.setCardCvv("123");
        dto.setCardHolderName("VIREAK BUYER");

        List<Order> orders = checkoutService.processCheckout(testBuyer.getId(), dto, session);
        assertEquals(1, orders.size());
        Order order = orders.get(0);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        // Confirm and release funds from 7-day Platform Master Bank escrow
        escrowService.releaseEscrow(order, "Card payment confirmed for test");

        // Seller received produce payout + shipping
        BigDecimal expectedSellerPayout = order.getSubtotal().subtract(new BigDecimal("1.00")).add(order.getShippingFee());
        BigDecimal sellerBalance = walletService.getBalance(testSeller.getId());
        assertEquals(sellerInitial.add(expectedSellerPayout), sellerBalance);

        // Platform received $1.00 commission
        BigDecimal adminBalance = walletService.getBalance(testAdmin.getId());
        assertEquals(adminInitial.add(new BigDecimal("1.00")), adminBalance);
    }
}
