package com.example.khmer_market.business;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.exception.ResourceNotFoundException;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.service.ReviewService;
import com.example.khmer_market.web.dto.ReviewDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ReviewEligibilityTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPackageRepository productPackageRepository;

    private User buyer;
    private User otherBuyer;
    private OrderItem orderItem;
    private Order order;

    @BeforeEach
    void setUp() {
        User seller = new User();
        seller.setUsername("reviewseller");
        seller.setEmail("reviewseller@example.com");
        seller.setPassword("Password123456!");
        seller.setRole(UserRole.FARMER);
        seller.setStatus(UserStatus.ACTIVE);
        seller = userRepository.saveAndFlush(seller);

        buyer = new User();
        buyer.setUsername("reviewbuyer");
        buyer.setEmail("reviewbuyer@example.com");
        buyer.setPassword("Password123456!");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.saveAndFlush(buyer);

        otherBuyer = new User();
        otherBuyer.setUsername("otherbuyer");
        otherBuyer.setEmail("otherbuyer@example.com");
        otherBuyer.setPassword("Password123456!");
        otherBuyer.setRole(UserRole.BUYER);
        otherBuyer.setStatus(UserStatus.ACTIVE);
        otherBuyer = userRepository.saveAndFlush(otherBuyer);

        Address addr = new Address();
        addr.setUser(buyer);
        addr.setName("Buyer");
        addr.setPhone("012345678");
        addr.setProvince("Siem Reap");
        addr = addressRepository.saveAndFlush(addr);

        Product prod = new Product();
        prod.setSeller(seller);
        prod.setName("Jasmine Rice");
        prod = productRepository.saveAndFlush(prod);

        ProductPackage pkg = new ProductPackage();
        pkg.setProduct(prod);
        pkg.setPackageName("50kg Bag");
        pkg.setPrice(new BigDecimal("45.00"));
        pkg = productPackageRepository.saveAndFlush(pkg);

        order = new Order();
        order.setBuyer(buyer);
        order.setAddress(addr);
        order.setOrderNumber("KM-TEST-REV01");
        order.setTotalAmount(new BigDecimal("45.00"));
        order.setStatus(OrderStatus.DELIVERED); // Marked delivered
        order = orderRepository.saveAndFlush(order);

        orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProductPackage(pkg);
        orderItem.setSeller(seller);
        orderItem.setPackageName("50kg Bag");
        orderItem.setUnitPrice(new BigDecimal("45.00"));
        orderItem.setQuantity(1);
        orderItem.setSubtotal(new BigDecimal("45.00"));
        orderItem = orderItemRepository.saveAndFlush(orderItem);
    }

    @Test
    @DisplayName("Buyer can submit review for delivered order item")
    void testEligibleReviewSubmission() {
        ReviewDto dto = new ReviewDto();
        dto.setOrderItemId(orderItem.getId());
        dto.setRating(5);
        dto.setComment("Delicious fragrant rice! Highly recommended.");

        Review review = reviewService.addReview(buyer.getId(), dto);
        assertNotNull(review);
        assertEquals(5, review.getRating());
        assertEquals(orderItem.getProductPackage().getProduct().getId(), review.getProduct().getId(),
                "Product ID must be derived safely from order_item");
    }

    @Test
    @DisplayName("Buyer can submit review for confirmed (paid) order item right after payment")
    void testEligibleReviewSubmissionOnConfirmedOrder() {
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.saveAndFlush(order);

        ReviewDto dto = new ReviewDto();
        dto.setOrderItemId(orderItem.getId());
        dto.setRating(5);
        dto.setComment("Fast checkout and easy payment!");

        Review review = reviewService.addReview(buyer.getId(), dto);
        assertNotNull(review);
        assertEquals(5, review.getRating());
    }

    @Test
    @DisplayName("Review rejected if order status is not delivered or confirmed")
    void testReviewRejectedIfNotDelivered() {
        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.saveAndFlush(order);

        ReviewDto dto = new ReviewDto();
        dto.setOrderItemId(orderItem.getId());
        dto.setRating(5);

        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(buyer.getId(), dto));
    }

    @Test
    @DisplayName("IDOR prevention: Other buyer cannot review another user's order item")
    void testOtherBuyerCannotReview() {
        ReviewDto dto = new ReviewDto();
        dto.setOrderItemId(orderItem.getId());
        dto.setRating(5);

        assertThrows(ResourceNotFoundException.class, () -> reviewService.addReview(otherBuyer.getId(), dto));
    }

    @Test
    @DisplayName("Duplicate review on same order item is rejected")
    void testDuplicateReviewRejected() {
        ReviewDto dto = new ReviewDto();
        dto.setOrderItemId(orderItem.getId());
        dto.setRating(4);

        reviewService.addReview(buyer.getId(), dto);

        // Attempt second review on same item
        ReviewDto dto2 = new ReviewDto();
        dto2.setOrderItemId(orderItem.getId());
        dto2.setRating(5);

        assertThrows(IllegalArgumentException.class, () -> reviewService.addReview(buyer.getId(), dto2));
    }
}
