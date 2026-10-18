package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.CartService;
import com.example.khmer_market.web.dto.CartItemViewDto;
import com.example.khmer_market.web.dto.CartViewDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CartIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private CartItemRepository cartItemRepository;

    @Autowired
    private FarmerProfileRepository farmerProfileRepository;

    private User seller;
    private CustomUserDetails sellerDetails;
    private User buyer;
    private CustomUserDetails buyerDetails;
    private ProductPackage pkg1;
    private ProductPackage pkg2;

    @BeforeEach
    void setUp() {
        seller = new User();
        seller.setUsername("cart_test_farmer");
        seller.setEmail("farmer@carttest.kh");
        seller.setPassword("Password123!");
        seller.setRole(UserRole.FARMER);
        seller.setStatus(UserStatus.ACTIVE);
        seller = userRepository.saveAndFlush(seller);
        sellerDetails = new CustomUserDetails(seller);

        FarmerProfile profile = new FarmerProfile();
        profile.setUser(seller);
        profile.setFarmerName("Sokha Farmer");
        profile.setFarmName("Angkor Organic Farm");
        profile.setPhone("012345678");
        profile.setProvince("Battambang");
        profile.setAddress("Street 5, Sangkat 2");
        farmerProfileRepository.saveAndFlush(profile);
        seller.setFarmerProfile(profile);

        buyer = new User();
        buyer.setUsername("cart_test_buyer");
        buyer.setEmail("buyer@carttest.kh");
        buyer.setPassword("Password123!");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.saveAndFlush(buyer);

        buyerDetails = new CustomUserDetails(buyer);

        Product product = new Product();
        product.setSeller(seller);
        product.setName("Battambang Jasmine Rice");
        product = productRepository.saveAndFlush(product);

        pkg1 = new ProductPackage();
        pkg1.setProduct(product);
        pkg1.setPackageName("5kg Pack");
        pkg1.setPrice(new BigDecimal("12.50"));
        pkg1 = productPackageRepository.saveAndFlush(pkg1);

        Inventory inv1 = new Inventory(pkg1, 50, 5);
        inventoryRepository.saveAndFlush(inv1);
        pkg1.setInventory(inv1);

        pkg2 = new ProductPackage();
        pkg2.setProduct(product);
        pkg2.setPackageName("10kg Pack");
        pkg2.setPrice(new BigDecimal("22.00"));
        pkg2 = productPackageRepository.saveAndFlush(pkg2);

        Inventory inv2 = new Inventory(pkg2, 30, 5);
        inventoryRepository.saveAndFlush(inv2);
        pkg2.setInventory(inv2);
    }

    @Test
    @DisplayName("Verify getCartItemCount and CartItemViewDto properties")
    void testCartItemCountAndDtoProperties() {
        assertEquals(0, cartService.getCartItemCount(buyer.getId()), "Initial cart count should be 0");

        // Add 3 units of pkg1
        cartService.addToCart(buyer.getId(), pkg1.getId(), 3);
        assertEquals(3, cartService.getCartItemCount(buyer.getId()), "Cart count should be 3 after adding 3 units");

        // Add 2 units of pkg2
        cartService.addToCart(buyer.getId(), pkg2.getId(), 2);
        assertEquals(5, cartService.getCartItemCount(buyer.getId()), "Cart count should be 5 after adding 2 units of pkg2");

        // Verify CartViewDto
        CartViewDto view = cartService.getCartView(buyer.getId());
        assertNotNull(view);
        assertEquals(2, view.getItems().size());
        assertEquals(5, view.getTotalItemCount());
        assertFalse(view.isEmpty());
        assertFalse(view.getEmpty());

        for (CartItemViewDto item : view.getItems()) {
            assertNotNull(item.getCartItemId(), "getCartItemId() must not be null");
            assertEquals(item.getId(), item.getCartItemId(), "getCartItemId() must match getId()");
            assertNotNull(item.getSellerFarmName(), "getSellerFarmName() must be populated");
            assertEquals("Angkor Organic Farm", item.getSellerFarmName());
        }

        // Test updating quantity
        Long firstItemId = view.getItems().get(0).getCartItemId();
        cartService.updateItemQuantity(buyer.getId(), firstItemId, 5);
        assertEquals(7, cartService.getCartItemCount(buyer.getId()), "Cart count should be updated to 7");

        // Test removing item
        cartService.removeItem(buyer.getId(), firstItemId);
        assertEquals(2, cartService.getCartItemCount(buyer.getId()), "Cart count should be 2 after removing first item");
    }

    @Test
    @DisplayName("Verify GET /cart renders template successfully with items")
    void testViewCartMvcRendering() throws Exception {
        cartService.addToCart(buyer.getId(), pkg1.getId(), 2);

        mockMvc.perform(get("/cart")
                        .with(user(buyerDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/cart"))
                .andExpect(model().attributeExists("cart"))
                .andExpect(model().attributeExists("cartItemCount"))
                .andExpect(model().attribute("cartItemCount", 2));
    }

    @Test
    @DisplayName("Verify POST /cart/add adds item and redirects to /cart")
    void testAddToCartMvc() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(user(buyerDetails))
                        .with(csrf())
                        .param("packageId", pkg1.getId().toString())
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        assertEquals(2, cartService.getCartItemCount(buyer.getId()));
    }

    @Test
    @DisplayName("Verify GET /api/cart/count returns JSON count")
    void testGetCartCountApi() throws Exception {
        cartService.addToCart(buyer.getId(), pkg1.getId(), 4);

        mockMvc.perform(get("/api/cart/count")
                        .with(user(buyerDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    @DisplayName("Verify seller cannot add own product to cart (throws IllegalArgumentException)")
    void testSellerCannotAddOwnProductToCart() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                cartService.addToCart(seller.getId(), pkg1.getId(), 1)
        );
        assertEquals("You cannot purchase your own product", ex.getMessage());
    }

    @Test
    @DisplayName("Verify AJAX POST /cart/add rejects seller attempting to buy own product with 400 Bad Request")
    void testSellerCannotAddOwnProductViaAjax() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .header("X-Requested-With", "XMLHttpRequest")
                        .with(user(sellerDetails))
                        .with(csrf())
                        .param("packageId", pkg1.getId().toString())
                        .param("quantity", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot purchase your own product"));
    }

    @Test
    @DisplayName("Verify standard POST /cart/add redirects with error message when seller buys own product")
    void testSellerCannotAddOwnProductViaMvc() throws Exception {
        mockMvc.perform(post("/cart/add")
                        .with(user(sellerDetails))
                        .with(csrf())
                        .param("packageId", pkg1.getId().toString())
                        .param("quantity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "You cannot purchase your own product"));
    }
}
