package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.payment.PaymentProviderFactory;
import com.example.khmer_market.payment.balance.BalancePaymentProvider;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.service.CheckoutService;
import com.example.khmer_market.service.EscrowService;
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
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Transactional
class WalletAndBalancePaymentTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private WalletService walletService;

    @Autowired
    private EscrowService escrowService;

    @Autowired
    private BalancePaymentProvider balancePaymentProvider;

    @Autowired
    private PaymentProviderFactory paymentProviderFactory;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CartService cartService;

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

    private User testBuyer;
    private User testSeller;
    private Address testAddress;
    private ProductPackage testPackage;

    @BeforeEach
    void setUp() {
        testBuyer = new User();
        testBuyer.setUsername("wallet_buyer_" + System.nanoTime());
        testBuyer.setEmail("wallet_buyer_" + System.nanoTime() + "@market.kh");
        testBuyer.setPassword("EncodedPass123!");
        testBuyer.setFullName("Buyer Wallet Test");
        testBuyer.setRole(UserRole.BUYER);
        testBuyer.setStatus(UserStatus.ACTIVE);
        testBuyer.setBalance(new BigDecimal("0.00"));
        testBuyer = userRepository.saveAndFlush(testBuyer);

        testSeller = new User();
        testSeller.setUsername("wallet_seller_" + System.nanoTime());
        testSeller.setEmail("wallet_seller_" + System.nanoTime() + "@market.kh");
        testSeller.setPassword("EncodedPass123!");
        testSeller.setFullName("Seller Wallet Test");
        testSeller.setRole(UserRole.FARMER);
        testSeller.setStatus(UserStatus.ACTIVE);
        testSeller.setBalance(new BigDecimal("0.00"));
        testSeller = userRepository.saveAndFlush(testSeller);

        FarmerProfile profile = new FarmerProfile();
        profile.setUser(testSeller);
        profile.setFarmName("Angkor Organic Farm");
        profile.setFarmerName("Sokha Farmer");
        profile.setProvince("Siem Reap");
        farmerProfileRepository.saveAndFlush(profile);

        testAddress = new Address();
        testAddress.setUser(testBuyer);
        testAddress.setName("Buyer Wallet Test");
        testAddress.setPhone("012888999");
        testAddress.setProvince("Phnom Penh");
        testAddress.setAddress("House 12, Street 2004");
        testAddress = addressRepository.saveAndFlush(testAddress);

        Product product = new Product();
        product.setSeller(testSeller);
        product.setName("Organic Bok Choy " + System.nanoTime());
        product = productRepository.saveAndFlush(product);

        testPackage = new ProductPackage();
        testPackage.setProduct(product);
        testPackage.setPackageName("1kg Bundle");
        testPackage.setPrice(new BigDecimal("10.00"));
        testPackage = productPackageRepository.saveAndFlush(testPackage);

        Inventory inv = new Inventory(testPackage, 50, 0);
        inventoryRepository.saveAndFlush(inv);
    }

    @Test
    @DisplayName("BalancePaymentProvider is registered and initiates successfully")
    void testBalancePaymentProvider() {
        assertEquals(PaymentMethod.BALANCE, balancePaymentProvider.getMethod());
        assertEquals(balancePaymentProvider, paymentProviderFactory.getProvider(PaymentMethod.BALANCE));

        Order order = new Order();
        order.setTotalAmount(new BigDecimal("25.00"));
        var result = balancePaymentProvider.initiatePayment(order);
        assertTrue(result.isSuccess());
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("BAL-"));
    }

    @Test
    @DisplayName("User deposit and withdraw updates balance and creates wallet audit transactions")
    void testDepositAndWithdraw() {
        // 1. Deposit $150.00
        WalletTransaction depTx = walletService.deposit(testBuyer.getId(), new BigDecimal("150.00"), "KHQR Simulation", "TEST-DEP-01");
        assertNotNull(depTx);
        assertEquals(WalletTransactionType.DEPOSIT, depTx.getType());
        assertEquals(new BigDecimal("150.00"), depTx.getAmount());
        assertEquals(new BigDecimal("150.00"), depTx.getBalanceAfter());
        assertEquals(WalletTransactionStatus.COMPLETED, depTx.getStatus());

        BigDecimal balanceAfterDep = walletService.getBalance(testBuyer.getId());
        assertEquals(new BigDecimal("150.00"), balanceAfterDep);

        // 2. Withdraw $50.00
        WalletTransaction wdTx = walletService.withdraw(testBuyer.getId(), new BigDecimal("50.00"), "ABA Bank", "001234567", "Buyer Wallet Test");
        assertNotNull(wdTx);
        assertEquals(WalletTransactionType.WITHDRAW, wdTx.getType());
        assertEquals(new BigDecimal("50.00"), wdTx.getAmount());
        assertEquals(new BigDecimal("100.00"), wdTx.getBalanceAfter());

        BigDecimal balanceAfterWd = walletService.getBalance(testBuyer.getId());
        assertEquals(new BigDecimal("100.00"), balanceAfterWd);

        // 3. Transactions query
        List<WalletTransaction> history = walletService.getRecentTransactions(testBuyer.getId());
        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("Withdrawal throws IllegalArgumentException when amount exceeds current balance")
    void testWithdrawInsufficientBalance() {
        walletService.deposit(testBuyer.getId(), new BigDecimal("20.00"), "KHQR", null);

        assertThrows(IllegalArgumentException.class, () ->
                walletService.withdraw(testBuyer.getId(), new BigDecimal("50.00"), "Wing", "012345678", "Test")
        );
    }

    @Autowired(required = false)
    private com.example.khmer_market.service.StoreConfigService storeConfigService;

    @Test
    @DisplayName("Checkout with balance payment succeeds when balance is sufficient")
    void testCheckoutWithBalancePaymentSuccess() {
        // Buyer deposits $50.00
        walletService.deposit(testBuyer.getId(), new BigDecimal("50.00"), "Deposit", "DEP-INIT");
        assertEquals(new BigDecimal("50.00"), walletService.getBalance(testBuyer.getId()));

        // Add 2 packages of $10.00 -> $20.00 subtotal
        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 2);

        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(testAddress.getId());
        dto.setPaymentMethod("balance");
        dto.setCheckoutToken(token);

        List<Order> orders = checkoutService.processCheckout(testBuyer.getId(), dto, session);
        assertNotNull(orders);
        assertEquals(1, orders.size());

        Order order = orders.get(0);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        BigDecimal shipping = storeConfigService != null ? storeConfigService.getShippingFlatRate() : new BigDecimal("1.50");
        BigDecimal expectedTotal = new BigDecimal("20.00").add(shipping).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedTotal, order.getTotalAmount());

        // Check payment record
        Payment payment = order.getPayments().get(0);
        assertEquals(PaymentMethod.BALANCE, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());

        // Check buyer balance: $50.00 - expectedTotal
        BigDecimal remainingBuyerBalance = walletService.getBalance(testBuyer.getId());
        assertEquals(new BigDecimal("50.00").subtract(expectedTotal), remainingBuyerBalance);

        // Confirm and release escrow funds to seller
        escrowService.releaseEscrow(order, "Test release");

        // Check seller balance: credited (subtotal - commission) + shipping fee ($20.00 - $1.00 + $1.50 = $20.50)
        BigDecimal expectedSellerEarnings = new BigDecimal("20.00").subtract(new BigDecimal("1.00")).add(shipping).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal sellerBalance = walletService.getBalance(testSeller.getId());
        assertEquals(expectedSellerEarnings, sellerBalance);
    }

    @Test
    @DisplayName("Checkout with balance payment fails when balance is insufficient")
    void testCheckoutWithBalancePaymentInsufficient() {
        // Buyer only has $5.00
        walletService.deposit(testBuyer.getId(), new BigDecimal("5.00"), "Deposit", "DEP-LOW");

        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 2); // Requires $21.50

        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(testAddress.getId());
        dto.setPaymentMethod("balance");
        dto.setCheckoutToken(token);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                checkoutService.processCheckout(testBuyer.getId(), dto, session)
        );
        assertTrue(ex.getMessage().contains("Insufficient account balance"));
    }

    @Test
    @DisplayName("CheckoutDto validation passes when addressId is provided even if newAddress is empty")
    void testCheckoutDtoValidationWithSavedAddress() {
        jakarta.validation.Validator validator = jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator();

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(1L);
        dto.setPaymentMethod("balance");
        dto.setCheckoutToken("valid-token-123");
        // newAddress is empty / unpopulated, as happens when buyer selects saved address
        dto.setNewAddress(new com.example.khmer_market.web.dto.AddressDto());

        java.util.Set<jakarta.validation.ConstraintViolation<CheckoutDto>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Violations should be empty when saved address is used, found: " + violations);
    }

    @Test
    @DisplayName("CartViewDto correctly calculates multi-vendor shipping and grandTotal")
    void testCartViewDtoShippingCalculation() {
        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 1); // $10.00
        var view = cartService.getCartView(testBuyer.getId());
        assertNotNull(view);
        BigDecimal expectedShipping = storeConfigService != null ? storeConfigService.getShippingFlatRate() : new BigDecimal("1.50");
        assertEquals(new BigDecimal("10.00"), view.getSubtotal());
        assertEquals(expectedShipping, view.getShippingFee());
        assertEquals(new BigDecimal("10.00").add(expectedShipping), view.getGrandTotal());
    }

    @Test
    @DisplayName("Confirmation page renders coffee shop thermal receipt without error")
    void testConfirmationPageRendersCoffeeShopReceipt() throws Exception {
        walletService.deposit(testBuyer.getId(), new BigDecimal("50.00"), "Deposit", "DEP-TEST");
        cartService.addToCart(testBuyer.getId(), testPackage.getId(), 1);

        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(testAddress.getId());
        dto.setPaymentMethod("balance");
        dto.setCheckoutToken(token);

        List<Order> orders = checkoutService.processCheckout(testBuyer.getId(), dto, session);
        String orderNumber = orders.get(0).getOrderNumber();

        var customUserDetails = new com.example.khmer_market.security.CustomUserDetails(testBuyer);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/checkout/confirmation")
                        .param("orders", orderNumber)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(customUserDetails)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Print Receipt")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("OFFICIAL RECEIPT")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString(orderNumber)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("PAID IN FULL")));
    }
}
