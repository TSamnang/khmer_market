package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.exception.OutOfStockException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.service.CheckoutService;
import com.example.khmer_market.web.dto.AddressDto;
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
public class CheckoutConcurrencyTest {

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
    private CartItemRepository cartItemRepository;

    private User buyer;
    private User seller1;
    private User seller2;
    private ProductPackage pkg1;
    private ProductPackage pkg2;
    private Address address;

    @BeforeEach
    void setUp() {
        seller1 = new User();
        seller1.setUsername("checkout_seller1");
        seller1.setEmail("seller1@market.kh");
        seller1.setPassword("ValidPassword123!");
        seller1.setRole(UserRole.FARMER);
        seller1.setStatus(UserStatus.ACTIVE);
        seller1 = userRepository.saveAndFlush(seller1);

        seller2 = new User();
        seller2.setUsername("checkout_seller2");
        seller2.setEmail("seller2@market.kh");
        seller2.setPassword("ValidPassword123!");
        seller2.setRole(UserRole.FARMER);
        seller2.setStatus(UserStatus.ACTIVE);
        seller2 = userRepository.saveAndFlush(seller2);

        buyer = new User();
        buyer.setUsername("checkout_buyer");
        buyer.setEmail("buyer@market.kh");
        buyer.setPassword("ValidPassword123!");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.saveAndFlush(buyer);

        address = new Address();
        address.setUser(buyer);
        address.setName("Chann Buyer");
        address.setPhone("012888999");
        address.setProvince("Battambang");
        address = addressRepository.saveAndFlush(address);

        // Product 1 from Seller 1
        Product p1 = new Product();
        p1.setSeller(seller1);
        p1.setName("Battambang Rice");
        p1 = productRepository.saveAndFlush(p1);

        pkg1 = new ProductPackage();
        pkg1.setProduct(p1);
        pkg1.setPackageName("25kg Sack");
        pkg1.setPrice(new BigDecimal("20.00"));
        pkg1 = productPackageRepository.saveAndFlush(pkg1);

        Inventory inv1 = new Inventory(pkg1, 10, 2);
        inv1 = inventoryRepository.saveAndFlush(inv1);
        pkg1.setInventory(inv1);

        // Product 2 from Seller 2
        Product p2 = new Product();
        p2.setSeller(seller2);
        p2.setName("Kampot Pepper");
        p2 = productRepository.saveAndFlush(p2);

        pkg2 = new ProductPackage();
        pkg2.setProduct(p2);
        pkg2.setPackageName("1kg Black Pepper");
        pkg2.setPrice(new BigDecimal("15.00"));
        pkg2 = productPackageRepository.saveAndFlush(pkg2);

        Inventory inv2 = new Inventory(pkg2, 5, 1);
        inv2 = inventoryRepository.saveAndFlush(inv2);
        pkg2.setInventory(inv2);
    }

    @Test
    @DisplayName("Multi-vendor split: Cart with items from 2 sellers splits into 2 distinct orders")
    void testMultiVendorPartitioning() {
        // Add items from both sellers into buyer's cart
        cartService.addToCart(buyer.getId(), pkg1.getId(), 2);
        cartService.addToCart(buyer.getId(), pkg2.getId(), 1);

        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(address.getId());
        dto.setPaymentMethod("card");
        dto.setCheckoutToken(token);

        List<Order> orders = checkoutService.processCheckout(buyer.getId(), dto, session);

        // Assert 2 distinct orders created (1 per seller)
        assertEquals(2, orders.size(), "Multi-vendor checkout must split into 2 orders");

        // Verify reservations
        Inventory updatedInv1 = inventoryRepository.findByProductPackageId(pkg1.getId()).orElseThrow();
        assertEquals(2, updatedInv1.getReservedQuantity(), "Pkg 1 reserved quantity should be 2");

        Inventory updatedInv2 = inventoryRepository.findByProductPackageId(pkg2.getId()).orElseThrow();
        assertEquals(1, updatedInv2.getReservedQuantity(), "Pkg 2 reserved quantity should be 1");

        // Verify cart is cleared
        Cart cart = cartService.getOrCreateCart(buyer.getId());
        assertEquals(0, cartItemRepository.countByCartId(cart.getId()), "Cart must be cleared after checkout");
    }

    @Test
    @DisplayName("Single-use token prevents double checkout execution")
    void testSingleUseCheckoutToken() {
        cartService.addToCart(buyer.getId(), pkg1.getId(), 1);

        MockHttpSession session = new MockHttpSession();
        String token = checkoutService.generateCheckoutToken(session);

        CheckoutDto dto = new CheckoutDto();
        dto.setAddressId(address.getId());
        dto.setPaymentMethod("card");
        dto.setCheckoutToken(token);

        // First checkout succeeds
        List<Order> orders = checkoutService.processCheckout(buyer.getId(), dto, session);
        assertEquals(1, orders.size());

        // Second attempt with same token must fail
        assertThrows(IllegalArgumentException.class, () ->
                checkoutService.processCheckout(buyer.getId(), dto, session));
    }

    @Test
    @DisplayName("Checkout rejects order if stock is insufficient")
    void testCheckoutRejectsInsufficientStock() {
        // Only 5 units available for pkg2, try adding 6
        assertThrows(OutOfStockException.class, () ->
                cartService.addToCart(buyer.getId(), pkg2.getId(), 6));
    }
}
