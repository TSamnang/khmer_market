package com.example.khmer_market.payment.khqr;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.entity.Payment;
import com.example.khmer_market.domain.enums.OrderStatus;
import com.example.khmer_market.domain.enums.PaymentStatus;
import com.example.khmer_market.domain.enums.UserRole;
import com.example.khmer_market.repository.OrderRepository;
import com.example.khmer_market.repository.PaymentRepository;
import com.example.khmer_market.service.OrderStateMachine;
import com.example.khmer_market.util.HmacUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments/khqr")
public class KhqrWebhookController {

    private static final Logger log = LoggerFactory.getLogger(KhqrWebhookController.class);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStateMachine orderStateMachine;
    private final com.example.khmer_market.service.WalletService walletService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${market.security.webhook-secret}")
    private String webhookSecret;

    @org.springframework.beans.factory.annotation.Autowired
    public KhqrWebhookController(OrderRepository orderRepository,
                                 PaymentRepository paymentRepository,
                                 OrderStateMachine orderStateMachine,
                                 com.example.khmer_market.service.WalletService walletService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderStateMachine = orderStateMachine;
        this.walletService = walletService;
    }

    public KhqrWebhookController(OrderRepository orderRepository,
                                 PaymentRepository paymentRepository,
                                 OrderStateMachine orderStateMachine) {
        this(orderRepository, paymentRepository, orderStateMachine, null);
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<String> handleKhqrWebhook(@RequestBody String rawPayload,
                                                    @RequestHeader(value = "X-Signature", required = false) String signature) {

        if (signature == null || signature.isBlank()) {
            log.warn("Security Event: KHQR webhook rejected - missing X-Signature header");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
        }

        // Constant-time HMAC-SHA256 signature verification
        if (!HmacUtils.verifyHmacSha256(rawPayload, webhookSecret, signature)) {
            log.warn("Security Event: KHQR webhook rejected - invalid HMAC-SHA256 signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String orderNumber = root.path("orderNumber").asText();
            String transactionId = root.path("transactionId").asText();
            String status = root.path("status").asText();

            if (orderNumber == null || orderNumber.isBlank()) {
                return ResponseEntity.badRequest().body("orderNumber required");
            }

            Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
            if (orderOpt.isEmpty()) {
                log.warn("KHQR webhook received for unknown order: {}", orderNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found");
            }

            Order order = orderOpt.get();

            // Find payment row
            Payment payment = order.getPayments().stream()
                    .findFirst()
                    .orElse(null);

            if (payment == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No payment associated with order");
            }

            // Idempotency: If already paid, return 200 immediately
            if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                log.info("Idempotent webhook hit: Order {} already marked PAID", orderNumber);
                return ResponseEntity.ok("Already processed");
            }

            if ("PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setPaymentDate(LocalDateTime.now());
                if (transactionId != null && !transactionId.isBlank()) {
                    payment.setTransactionId(transactionId);
                }
                paymentRepository.save(payment);

                // Move order from PENDING -> CONFIRMED
                if (order.getStatus() == OrderStatus.PENDING) {
                    orderStateMachine.confirmOrder(order, UserRole.ADMIN);
                }

                // Disburse seller earnings (produce + shipping) and platform commission
                if (walletService != null && order.getSeller() != null) {
                    walletService.distributeOrderEarnings(order.getSeller().getId(), order.getSubtotal(), order.getShippingFee(), order.getOrderNumber());
                }

                log.info("Security Event: Payment confirmed via KHQR webhook for Order {}", orderNumber);
                return ResponseEntity.ok("Payment confirmed");
            } else if ("FAILED".equalsIgnoreCase(status)) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.warn("KHQR webhook reported payment failure for Order {}", orderNumber);
                return ResponseEntity.ok("Payment marked failed");
            }

            return ResponseEntity.ok("Status acknowledged");

        } catch (Exception e) {
            log.error("Failed to parse KHQR webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Malformed payload");
        }
    }
}
