package com.example.khmer_market.payment.khqr;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.repository.OrderRepository;
import com.example.khmer_market.util.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/dev/payments/simulate")
@Profile("dev")
public class KhqrSimulateController {

    private final OrderRepository orderRepository;
    private final KhqrWebhookController webhookController;

    @Value("${market.security.webhook-secret}")
    private String webhookSecret;

    public KhqrSimulateController(OrderRepository orderRepository,
                                  KhqrWebhookController webhookController) {
        this.orderRepository = orderRepository;
        this.webhookController = webhookController;
    }

    @PostMapping("/{orderNumber}")
    public ResponseEntity<String> simulatePaymentSuccess(@PathVariable String orderNumber) {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String txId = "SIM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String payload = String.format("{\"orderNumber\":\"%s\",\"transactionId\":\"%s\",\"amount\":%.2f,\"status\":\"PAID\"}",
                orderNumber, txId, orderOpt.get().getTotalAmount());

        String signature = HmacUtils.calculateHmacSha256(payload, webhookSecret);

        return webhookController.handleKhqrWebhook(payload, signature);
    }
}
