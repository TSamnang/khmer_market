package com.example.khmer_market.security;

import com.example.khmer_market.domain.entity.*;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.domain.enums.PaymentStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.domain.enums.UserStatus;
import com.example.khmer_market.repository.*;
import com.example.khmer_market.util.HmacUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class WebhookHmacSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Value("${market.security.webhook-secret}")
    private String webhookSecret;

    private Order order;

    @BeforeEach
    void setUp() {
        User buyer = new User();
        buyer.setUsername("webhookbuyer");
        buyer.setEmail("webhook@example.com");
        buyer.setPassword("Password123456!");
        buyer.setRole(UserRole.BUYER);
        buyer.setStatus(UserStatus.ACTIVE);
        buyer = userRepository.saveAndFlush(buyer);

        Address addr = new Address();
        addr.setUser(buyer);
        addr.setName("Buyer");
        addr.setPhone("012345678");
        addr.setProvince("Phnom Penh");
        addr = addressRepository.saveAndFlush(addr);

        order = new Order();
        order.setBuyer(buyer);
        order.setAddress(addr);
        order.setOrderNumber("KM-TEST-WH01");
        order.setTotalAmount(new BigDecimal("25.00"));
        order.setStatus(OrderStatus.PENDING);
        order = orderRepository.saveAndFlush(order);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.KHQR);
        payment.setAmount(new BigDecimal("25.00"));
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment = paymentRepository.saveAndFlush(payment);
        order.addPayment(payment);
    }

    @Test
    @DisplayName("Webhook request without X-Signature header is rejected with 401 Unauthorized")
    void testWebhookMissingSignature() throws Exception {
        String payload = "{\"orderNumber\":\"KM-TEST-WH01\",\"status\":\"PAID\"}";
        mockMvc.perform(post("/api/payments/khqr/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Webhook request with invalid HMAC signature is rejected with 401 Unauthorized")
    void testWebhookInvalidSignature() throws Exception {
        String payload = "{\"orderNumber\":\"KM-TEST-WH01\",\"status\":\"PAID\"}";
        mockMvc.perform(post("/api/payments/khqr/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", "invalid-hmac-signature-12345")
                .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Webhook request with valid HMAC signature successfully marks order paid and confirmed")
    void testWebhookValidSignature() throws Exception {
        String payload = "{\"orderNumber\":\"KM-TEST-WH01\",\"transactionId\":\"TX-KHQR-999\",\"status\":\"PAID\"}";
        String signature = HmacUtils.calculateHmacSha256(payload, webhookSecret);

        mockMvc.perform(post("/api/payments/khqr/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", signature)
                .content(payload))
                .andExpect(status().isOk());

        Order updatedOrder = orderRepository.findByOrderNumber("KM-TEST-WH01").orElseThrow();
        assertEquals(OrderStatus.CONFIRMED, updatedOrder.getStatus(), "Paid order must move to CONFIRMED");

        Payment payment = updatedOrder.getPayments().get(0);
        assertEquals(PaymentStatus.PAID, payment.getPaymentStatus());
        assertEquals("TX-KHQR-999", payment.getTransactionId());

        // Test Idempotency: Second request with same payload should succeed without error
        mockMvc.perform(post("/api/payments/khqr/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", signature)
                .content(payload))
                .andExpect(status().isOk());
    }
}
