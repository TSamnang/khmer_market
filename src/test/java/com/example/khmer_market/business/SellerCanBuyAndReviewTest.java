package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.*;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.security.CustomUserDetails;
import com.example.khmer_market.service.OrderService;
import com.example.khmer_market.service.ReviewService;
import com.example.khmer_market.web.dto.ReviewDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SellerCanBuyAndReviewTest {

    @Autowired
    private MockMvc mockMvc;

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
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewService reviewService;

    @Test
    @DisplayName("Seller with FARMER role can access /buyer/orders and view purchase history")
    void testSellerCanAccessBuyerOrders() throws Exception {
        User seller = new User();
        seller.setUsername("farmer_buyer_" + UUID.randomUUID().toString().substring(0, 8));
        seller.setEmail(seller.getUsername() + "@market.kh");
        seller.setPassword("Password123!");
        seller.setRole(UserRole.FARMER);
        seller.setStatus(UserStatus.ACTIVE);
        seller = userRepository.save(seller);

        CustomUserDetails userDetails = new CustomUserDetails(seller);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        MvcResult result = mockMvc.perform(get("/buyer/orders")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(view().name("buyer/orders"))
                .andExpect(model().attributeExists("orders"))
                .andExpect(model().attributeExists("toReviewCount"))
                .andReturn();

        String html = result.getResponse().getContentAsString();
        // Verify seller can see the link to switch back to sales orders to fulfil
        assertThat(html).contains("/farmer/orders");
    }

    @Test
    @DisplayName("Delivered order appears under 'to_review' tab, prompts review on confirm delivery, and disappears after review")
    void testToReviewFlowAndDeliveryPrompt() throws Exception {
        // 1. Create a buyer and a seller
        User seller = new User();
        seller.setUsername("seller_" + UUID.randomUUID().toString().substring(0, 8));
        seller.setEmail(seller.getUsername() + "@market.kh");
        seller.setPassword("Password123!");
        seller.setRole(UserRole.FARMER);
        seller.setStatus(UserStatus.ACTIVE);
        seller = userRepository.save(seller);

        User buyer = new User();
        buyer.setUsername("buyer_" + UUID.randomUUID().toString().substring(0, 8));
        buyer.setEmail(buyer.getUsername() + "@market.kh");
        buyer.setPassword("Password123!");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.save(buyer);

        Category category = new Category();
        category.setName("Fresh Fruits " + UUID.randomUUID().toString().substring(0, 5));
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Battambang Oranges");
        product.setSeller(seller);
        product.setCategory(category);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        ProductPackage pkg = new ProductPackage();
        pkg.setProduct(product);
        pkg.setPackageName("5kg Basket");
        pkg.setPrice(BigDecimal.valueOf(12.50));
        pkg.setStatus(PackageStatus.ACTIVE);
        pkg = productPackageRepository.save(pkg);

        Address address = new Address();
        address.setUser(buyer);
        address.setName("Buyer Name");
        address.setPhone("012345678");
        address.setProvince("Siem Reap");
        address.setAddress("House 123, Svay Dangkum");
        address = addressRepository.save(address);

        // 2. Create Order in SHIPPED status
        Order order = new Order();
        order.setOrderNumber("KM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setBuyer(buyer);
        order.setAddress(address);
        order.setSubtotal(BigDecimal.valueOf(12.50));
        order.setShippingFee(BigDecimal.valueOf(1.50));
        order.setTotalAmount(BigDecimal.valueOf(14.00));
        order.setStatus(OrderStatus.SHIPPED);
        order.setEscrowStatus("HELD");
        order = orderRepository.save(order);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductPackage(pkg);
        item.setSeller(seller);
        item.setPackageName(pkg.getPackageName());
        item.setUnitPrice(pkg.getPrice());
        item.setQuantity(1);
        item.setSubtotal(pkg.getPrice());
        item = orderItemRepository.save(item);

        CustomUserDetails buyerDetails = new CustomUserDetails(buyer);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(buyerDetails, null, buyerDetails.getAuthorities());

        // 3. Confirm delivery -> triggers autoOpenReview flash attribute
        mockMvc.perform(post("/buyer/orders/" + order.getId() + "/confirm-delivery")
                        .with(authentication(auth))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/orders/" + order.getId()))
                .andExpect(flash().attribute("autoOpenReview", true))
                .andExpect(flash().attributeExists("successMessage"));

        // 4. Verify order is now DELIVERED and unreviewed count is 1
        assertThat(orderService.countBuyerUnreviewedOrders(buyer.getId())).isEqualTo(1);

        // 5. Query /buyer/orders?status=to_review
        MvcResult toReviewResult = mockMvc.perform(get("/buyer/orders")
                        .param("status", "to_review")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isToReview", true))
                .andExpect(model().attribute("toReviewCount", 1L))
                .andReturn();

        String toReviewHtml = toReviewResult.getResponse().getContentAsString();
        assertThat(toReviewHtml).contains(order.getOrderNumber());

        // 6. Submit a 5-star review for the delivered item
        ReviewDto reviewDto = new ReviewDto();
        reviewDto.setOrderItemId(item.getId());
        reviewDto.setRating(5);
        reviewDto.setComment("Sweetest oranges from Battambang!");

        mockMvc.perform(post("/buyer/orders/" + order.getId() + "/review")
                        .with(authentication(auth))
                        .with(csrf())
                        .flashAttr("reviewDto", reviewDto))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/orders/" + order.getId()))
                .andExpect(flash().attribute("successMessage", "Thank you! All items in this order have been reviewed."));

        // 7. Verify review saved and unreviewed count dropped to 0
        assertThat(reviewRepository.existsByOrderItemId(item.getId())).isTrue();
        assertThat(orderService.countBuyerUnreviewedOrders(buyer.getId())).isEqualTo(0);

        // 8. /buyer/orders?status=to_review should now be empty of unreviewed orders
        mockMvc.perform(get("/buyer/orders")
                        .param("status", "to_review")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("toReviewCount", 0L));
    }
}
