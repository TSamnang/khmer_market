package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.service.CheckoutService;
import com.example.khmer_market.service.EscrowService;
import com.example.khmer_market.service.OrderStateMachine;
import com.example.khmer_market.service.WalletService;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class OrderRefundAndAdminWithdrawalTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private OrderStateMachine orderStateMachine;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPackageRepository productPackageRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    private User testAdmin;
    private User testSeller;
    private User testBuyer;
    private ProductPackage testPackage;
    private Address testAddress;

    @BeforeEach
    void setUp() {
        testAdmin = userRepository.findFirstByRole(UserRole.ADMIN).orElseGet(() -> {
            User admin = new User();
            admin.setUsername("refund_admin");
            admin.setEmail("admin@refundtest.kh");
            admin.setPassword("AdminPass123!");
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.creditBalance(new BigDecimal("100.00"));
            return userRepository.saveAndFlush(admin);
        });

        testSeller = new User();
        testSeller.setUsername("refund_farmer");
        testSeller.setEmail("farmer@refundtest.kh");
        testSeller.setPassword("FarmerPass123!");
        testSeller.setRole(UserRole.FARMER);
        testSeller.setStatus(UserStatus.ACTIVE);
        testSeller = userRepository.saveAndFlush(testSeller);

        testBuyer = new User();
        testBuyer.setUsername("refund_buyer");
        testBuyer.setEmail("buyer@refundtest.kh");
        testBuyer.setPassword("BuyerPass123!");
        testBuyer.setRole(UserRole.BUYER);
        testBuyer.setStatus(UserStatus.ACTIVE);
        testBuyer.creditBalance(new BigDecimal("100.00"));
        testBuyer = userRepository.saveAndFlush(testBuyer);

        testAddress = new Address();
        testAddress.setUser(testBuyer);
        testAddress.setName("Chenda Buyer");
        testAddress.setPhone("012888999");
        testAddress.setProvince("Phnom Penh");
        testAddress.setAddress("House 12, Street 2004");
        testAddress = addressRepository.saveAndFlush(testAddress);

        Product product = new Product();
        product.setSeller(testSeller);
        product.setName("Siem Reap Organic Honey");
        product = productRepository.saveAndFlush(product);

        testPackage = new ProductPackage();
        testPackage.setProduct(product);
        testPackage.setPackageName("500ml Jar");
        testPackage.setPrice(new BigDecimal("10.00"));
        testPackage = productPackageRepository.saveAndFlush(testPackage);

        Inventory inv = new Inventory(testPackage, 50, 0);
        inventoryRepository.saveAndFlush(inv);
        testPackage.setInventory(inv);
    }

    @Test
    @DisplayName("Platform Owner withdraws profit without paying administrative fee ($0 fee)")
    void testPlatformOwnerWithdrawsWithZeroFee() {
        testAdmin.setBalance(new BigDecimal("100.00"));
        userRepository.saveAndFlush(testAdmin);

        WalletTransaction tx = walletService.withdraw(
                testAdmin.getId(),
                new BigDecimal("40.00"),
                "ABA Bank",
                "001 234 567",
                "KHMER MARKET PLATFORM OWNER"
        );

        assertNotNull(tx);
        assertEquals(new BigDecimal("40.00"), tx.getAmount());
        assertEquals(WalletTransactionType.WITHDRAW, tx.getType());

        // Platform owner gets 100% of payout: remaining balance should be exactly $60.00
        BigDecimal adminBal = walletService.getBalance(testAdmin.getId());
        assertEquals(new BigDecimal("60.00"), adminBal);

        // Verify description confirms $0 fee
        assertTrue(tx.getDescription().contains("Fee: $0.00"), "Description should state Fee: $0.00");
    }

    @Test
    @DisplayName("Cancelling a paid order automatically refunds buyer 100%, claws back seller earnings and platform commission")
    void testPaidOrderCancellationTriggersFullRefundAndReversals() {
        BigDecimal initialBuyerBalance = walletService.getBalance(testBuyer.getId());
        BigDecimal initialSellerBalance = walletService.getBalance(testSeller.getId());
        BigDecimal initialAdminBalance = walletService.getBalance(testAdmin.getId());

        // 1. Buyer purchases 2 packages ($20.00 produce) using account balance
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
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        BigDecimal totalPaid = order.getTotalAmount();
        BigDecimal shippingFee = order.getShippingFee();
        BigDecimal expectedCommission = new BigDecimal("1.00"); // 5% of $20.00
        BigDecimal expectedSellerPayout = new BigDecimal("19.00").add(shippingFee);

        // Confirm and release escrow funds to seller & admin
        escrowService.releaseEscrow(order, "Order confirmed for test");

        // Verify post-release balances
        assertEquals(initialBuyerBalance.subtract(totalPaid), walletService.getBalance(testBuyer.getId()));
        assertEquals(initialSellerBalance.add(expectedSellerPayout), walletService.getBalance(testSeller.getId()));
        assertEquals(initialAdminBalance.add(expectedCommission), walletService.getBalance(testAdmin.getId()));

        // 2. Cancel the order (e.g. farmer unable to fulfill or admin dispute resolution)
        orderStateMachine.cancelOrder(order, UserRole.ADMIN, "Customer requested order cancellation before shipment");

        // 3. Verify order status and payment status
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        Payment payment = paymentRepository.findByOrderId(order.getId()).get(0);
        assertEquals(PaymentStatus.REFUNDED, payment.getPaymentStatus());

        // 4. Verify financial balances are completely restored
        // Buyer got 100% refund
        assertEquals(initialBuyerBalance, walletService.getBalance(testBuyer.getId()), "Buyer balance should be 100% refunded");

        // Seller earnings clawed back
        assertEquals(initialSellerBalance, walletService.getBalance(testSeller.getId()), "Seller balance should be returned to initial");

        // Admin commission clawed back
        assertEquals(initialAdminBalance, walletService.getBalance(testAdmin.getId()), "Admin commission should be reversed");

        // 5. Verify wallet transactions contain REFUND for buyer
        List<WalletTransaction> buyerTxs = walletTransactionRepository.findTop10ByUserIdOrderByCreatedAtDesc(testBuyer.getId());
        boolean hasRefundTx = buyerTxs.stream()
                .anyMatch(t -> t.getType() == WalletTransactionType.REFUND && t.getAmount().compareTo(totalPaid) == 0);
        assertTrue(hasRefundTx, "Buyer should have a completed REFUND transaction matching the full order total");

        // 6. Verify inventory reservation was released
        Inventory inv = inventoryRepository.findByProductPackageId(testPackage.getId()).orElseThrow();
        assertEquals(0, inv.getReservedQuantity(), "Reserved inventory quantity must be released to 0");
    }
}
