package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.exception.InvalidOrderTransitionException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.*;
import com.example.khmer_market.web.dto.CheckoutDto;
import com.example.khmer_market.web.dto.StoreConfigDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PlatformBankAndEscrowTest {

    @Autowired
    private StoreConfigService storeConfigService;

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderStateMachine orderStateMachine;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPackageRepository productPackageRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

    private User testBuyer;
    private User testSeller;
    private User testAdmin;
    private Address testAddress;
    private ProductPackage testPackage;

    @BeforeEach
    void setUp() {
        testBuyer = new User();
        testBuyer.setUsername("escrow_buyer_" + System.currentTimeMillis());
        testBuyer.setEmail("buyer_" + System.currentTimeMillis() + "@market.kh");
        testBuyer.setPassword("hash");
        testBuyer.setRole(UserRole.BUYER);
        testBuyer.setStatus(UserStatus.ACTIVE);
        testBuyer.setBalance(new BigDecimal("100.00"));
        testBuyer = userRepository.saveAndFlush(testBuyer);

        testSeller = new User();
        testSeller.setUsername("escrow_seller_" + System.currentTimeMillis());
        testSeller.setEmail("seller_" + System.currentTimeMillis() + "@market.kh");
        testSeller.setPassword("hash");
        testSeller.setRole(UserRole.FARMER);
        testSeller.setStatus(UserStatus.ACTIVE);
        testSeller.setBalance(BigDecimal.ZERO);
        testSeller = userRepository.saveAndFlush(testSeller);

        testAdmin = userRepository.findFirstByRole(UserRole.ADMIN).orElseGet(() -> {
            User admin = new User();
            admin.setUsername("escrow_admin");
            admin.setEmail("admin_escrow@market.kh");
            admin.setPassword("hash");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setBalance(BigDecimal.ZERO);
            return userRepository.saveAndFlush(admin);
        });

        testAddress = new Address();
        testAddress.setUser(testBuyer);
        testAddress.setName("Sokha Escrow");
        testAddress.setPhone("012333444");
        testAddress.setProvince("Battambang");
        testAddress.setAddress("Street 101, Sangkat 2");
        testAddress = addressRepository.saveAndFlush(testAddress);

        Category category = new Category();
        category.setName("Escrow Test Produce " + System.currentTimeMillis());
        category = categoryRepository.saveAndFlush(category);

        Product product = new Product();
        product.setSeller(testSeller);
        product.setCategory(category);
        product.setName("Battambang Jasmine Rice");
        product = productRepository.saveAndFlush(product);

        testPackage = new ProductPackage();
        testPackage.setProduct(product);
        testPackage.setPackageName("5kg Sack");
        testPackage.setPrice(new BigDecimal("10.00"));
        testPackage = productPackageRepository.saveAndFlush(testPackage);

        Inventory inv = new Inventory(testPackage, 100, 0);
        inventoryRepository.saveAndFlush(inv);
        testPackage.setInventory(inv);
    }

    @Test
    @DisplayName("Admin can configure and update Platform Master Bank & Gateway API settings")
    void testPlatformMasterBankConfigUpdate() {
        StoreConfigDto dto = new StoreConfigDto();
        dto.setBankName("Bakong NBC (KHQR)");
        dto.setBankAccountNumber("009 777 888");
        dto.setBankAccountName("KHMER AGRICULTURAL MARKET MASTER ACCOUNT");
        dto.setBankBranch("Phnom Penh NBC Tower");
        dto.setBakongMerchantId("khmer_market@aba");
        dto.setBankApiKey("pk_live_bakong_999888");
        dto.setBankApiSecret("sk_live_bakong_sec_777666");
        dto.setBankWebhookSecret("whsec_bakong_111222");
        dto.setBankEnvironment("LIVE");
        dto.setEscrowHoldDays(7);
        dto.setCommissionPercentage(new BigDecimal("5.00"));

        storeConfigService.updateBankConfig(dto, "admin", "127.0.0.1");

        assertEquals("Bakong NBC (KHQR)", storeConfigService.getBankName());
        assertEquals("009 777 888", storeConfigService.getBankAccountNumber());
        assertEquals("KHMER AGRICULTURAL MARKET MASTER ACCOUNT", storeConfigService.getBankAccountName());
        assertEquals("LIVE", storeConfigService.getBankEnvironment());
        assertEquals(7, storeConfigService.getEscrowHoldDays());
    }

    @Test
    @DisplayName("Order payment places funds in 7-day Escrow; Buyer cannot refund during 7-day holding period")
    void testEscrowHoldBlocksBuyerCancellation() {
        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 2); // $20.00 produce
        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(testAddress.getId());
        dto.setPaymentMethod("balance");
        dto.setCheckoutToken(token);

        List<Order> orders = checkoutService.processCheckout(testBuyer.getId(), dto, session);
        assertEquals(1, orders.size());
        Order order = orders.get(0);

        // 1. Order is placed in Escrow (status HELD, release date in 7 days)
        assertEquals("HELD", order.getEscrowStatus());
        assertNotNull(order.getEscrowReleaseAt());
        assertTrue(order.isUnderEscrowHold());

        // 2. Seller balance is NOT yet converted (funds remain in Platform Master Bank Escrow)
        BigDecimal sellerBalanceDuringHold = walletService.getBalance(testSeller.getId());
        assertEquals(BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP), sellerBalanceDuringHold);

        // 3. Buyer cannot cancel or refund during 7-day escrow hold
        assertThrows(InvalidOrderTransitionException.class, () -> {
            orderStateMachine.cancelOrder(order, UserRole.BUYER, "Want refund");
        }, "Buyer should be prevented from refunding during the 7-day escrow window");
    }

    @Test
    @DisplayName("7-Day Auto-confirm or manual release converts Master Bank escrow into seller balance")
    void testEscrowReleaseConvertsBankFundsToSellerBalance() {
        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 2); // $20.00 produce
        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(testAddress.getId());
        dto.setPaymentMethod("balance");
        dto.setCheckoutToken(token);

        List<Order> orders = checkoutService.processCheckout(testBuyer.getId(), dto, session);
        Order order = orders.get(0);
        assertTrue(order.isUnderEscrowHold());

        BigDecimal adminInitial = walletService.getBalance(testAdmin.getId());

        // Simulate 7-day expiration by adjusting releaseAt into the past
        order.setEscrowReleaseAt(LocalDateTime.now().minusMinutes(5));
        orderRepository.saveAndFlush(order);

        // Run auto-confirm
        int confirmed = escrowService.autoConfirmExpiredEscrows();
        assertTrue(confirmed >= 1);

        // Reload order
        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals("RELEASED", reloaded.getEscrowStatus());
        assertNotNull(reloaded.getEscrowReleasedAt());

        // Seller balance should now be credited (produce - 5% commission + shipping)
        BigDecimal shipping = (reloaded.getShippingFee() != null) ? reloaded.getShippingFee() : new BigDecimal("1.50");
        BigDecimal expectedSellerEarnings = reloaded.getSubtotal().subtract(new BigDecimal("1.00")).add(shipping);
        assertEquals(expectedSellerEarnings, walletService.getBalance(testSeller.getId()));

        // Admin retains 5% commission ($1.00) in Master Bank
        BigDecimal adminNew = walletService.getBalance(testAdmin.getId());
        assertEquals(adminInitial.add(new BigDecimal("1.00")), adminNew);
    }
}
