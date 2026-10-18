package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.payment.PaymentProviderFactory;
import com.example.khmer_market.payment.PaymentResult;
import com.example.khmer_market.payment.card.CardPaymentProvider;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.service.CatalogService;
import com.example.khmer_market.service.CheckoutService;
import com.example.khmer_market.web.dto.CheckoutDto;
import com.example.khmer_market.web.dto.ProductCardDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
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
class CardPaymentAndSellerDetailsTest {

    @Autowired
    private CardPaymentProvider cardPaymentProvider;

    @Autowired
    private PaymentProviderFactory paymentProviderFactory;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private CartService cartService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPackageRepository productPackageRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    @Autowired
    private CatalogService catalogService;

    private Validator validator;

    @BeforeEach
    void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CardPaymentProvider correctly provides CARD method and successful authorization")
    void testCardPaymentProvider() {
        assertEquals(PaymentMethod.CARD, cardPaymentProvider.getMethod());

        Order testOrder = new Order();
        testOrder.setOrderNumber("KM-TEST-CARD-01");
        testOrder.setTotalAmount(new BigDecimal("35.00"));

        PaymentResult result = cardPaymentProvider.initiatePayment(testOrder);
        assertTrue(result.isSuccess());
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("CARD-"));
        assertTrue(result.getInstructions().contains("Credit / Debit Card"));
    }

    @Test
    @DisplayName("PaymentProviderFactory resolves CardPaymentProvider for PaymentMethod.CARD")
    void testPaymentProviderFactoryResolvesCard() {
        assertNotNull(paymentProviderFactory.getProvider(PaymentMethod.CARD));
        assertEquals(PaymentMethod.CARD, paymentProviderFactory.getProvider(PaymentMethod.CARD).getMethod());
    }

    @Test
    @DisplayName("CheckoutDto validation allows card and khqr, rejects cod and bank_transfer")
    void testCheckoutDtoValidation() {
        CheckoutDto validCardDto = new CheckoutDto();
        validCardDto.setPaymentMethod("card");
        validCardDto.setCheckoutToken("valid-token");
        assertTrue(validator.validateProperty(validCardDto, "paymentMethod").isEmpty(), "card should be valid");

        CheckoutDto validKhqrDto = new CheckoutDto();
        validKhqrDto.setPaymentMethod("khqr");
        validKhqrDto.setCheckoutToken("valid-token");
        assertTrue(validator.validateProperty(validKhqrDto, "paymentMethod").isEmpty(), "khqr should be valid");

        CheckoutDto invalidCodDto = new CheckoutDto();
        invalidCodDto.setPaymentMethod("cod");
        assertFalse(validator.validateProperty(invalidCodDto, "paymentMethod").isEmpty(), "cod must be rejected");

        CheckoutDto invalidBankDto = new CheckoutDto();
        invalidBankDto.setPaymentMethod("bank_transfer");
        assertFalse(validator.validateProperty(invalidBankDto, "paymentMethod").isEmpty(), "bank_transfer must be rejected");
    }

    @Test
    @DisplayName("Card payment completes checkout immediately with PAID status and CONFIRMED order")
    void testCardPaymentCheckoutExecution() {
        // Create Seller and FarmerProfile
        User seller = new User();
        seller.setUsername("farmer_dara_" + System.currentTimeMillis());
        seller.setEmail("dara_" + System.currentTimeMillis() + "@market.kh");
        seller.setPassword("ValidSecret123!");
        seller.setFullName("Dara Meas");
        seller.setRole(UserRole.FARMER);
        seller = userRepository.saveAndFlush(seller);

        FarmerProfile profile = new FarmerProfile();
        profile.setUser(seller);
        profile.setFarmName("Dara Organic Farm");
        profile.setFarmerName("Dara Meas");
        profile.setProvince("Battambang");
        farmerProfileRepository.saveAndFlush(profile);

        // Create Buyer
        User buyer = new User();
        buyer.setUsername("buyer_test_" + System.currentTimeMillis());
        buyer.setEmail("buyer_" + System.currentTimeMillis() + "@market.kh");
        buyer.setPassword("ValidSecret123!");
        buyer.setFullName("Sokna Lim");
        buyer.setRole(UserRole.BUYER);
        buyer = userRepository.saveAndFlush(buyer);

        Address address = new Address();
        address.setUser(buyer);
        address.setName("Sokna Lim");
        address.setPhone("012333444");
        address.setProvince("Phnom Penh");
        address.setAddress("St 271, Sangkat Boeung Tumpun");
        address = addressRepository.saveAndFlush(address);

        // Create Product & Package
        Product product = new Product();
        product.setSeller(seller);
        product.setName("Battambang Jasmine Rice");
        product = productRepository.saveAndFlush(product);

        ProductPackage pkg = new ProductPackage();
        pkg.setProduct(product);
        pkg.setPackageName("5kg Sack");
        pkg.setPrice(new BigDecimal("7.50"));
        pkg = productPackageRepository.saveAndFlush(pkg);

        Inventory inv = new Inventory(pkg, 20, 0);
        inventoryRepository.saveAndFlush(inv);

        // Add to cart & Checkout with Card
        cartService.addToCart(buyer.getId(), pkg.getId(), 2);

        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(address.getId());
        dto.setPaymentMethod("card");
        dto.setCheckoutToken(token);
        dto.setCardNumber("4532 1111 2222 3333");
        dto.setCardHolderName("SOKNA LIM");
        dto.setCardExpiry("12/28");
        dto.setCardCvv("789");

        List<Order> orders = checkoutService.processCheckout(buyer.getId(), dto, session);
        assertEquals(1, orders.size());
        Order placedOrder = orders.get(0);

        // For CARD payment, order is immediately CONFIRMED and payment status is PAID
        assertEquals(OrderStatus.CONFIRMED, placedOrder.getStatus());
        assertEquals(1, placedOrder.getPayments().size());

        Payment payment = placedOrder.getPayments().get(0);
        assertEquals(PaymentMethod.CARD, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertNotNull(payment.getPaymentDate());
        assertTrue(payment.getTransactionId().startsWith("CARD-"));
    }

    @Test
    @DisplayName("ProductCardDto displays seller user details and farm name distinctly")
    void testProductCardDtoSellerDetails() {
        ProductCardDto dto = new ProductCardDto();
        dto.setId(101L);
        dto.setName("Fresh Mangoes");
        dto.setSellerId(55L);
        dto.setFarmName("Kampot Mango Orchards");
        dto.setSellerName("Vannak Khem");
        dto.setSellerUsername("vannak_farmer");
        dto.setProvince("Kampot");

        assertEquals("Kampot Mango Orchards", dto.getFarmName());
        assertEquals("Kampot Mango Orchards", dto.getSellerFarmName());
        assertEquals("Vannak Khem", dto.getSellerName());
        assertEquals("vannak_farmer", dto.getSellerUsername());
        assertEquals("Kampot", dto.getProvince());

        // Test fallback when sellerName is blank
        ProductCardDto fallbackDto = new ProductCardDto();
        fallbackDto.setSellerId(77L);
        fallbackDto.setSellerUsername("sok_user");
        assertEquals("sok_user", fallbackDto.getSellerName());
        assertEquals("sok_user", fallbackDto.getSellerUsername());
    }
}
