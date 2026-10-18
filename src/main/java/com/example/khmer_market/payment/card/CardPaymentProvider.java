package com.example.khmer_market.payment.card;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.payment.PaymentProvider;
import com.example.khmer_market.payment.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CardPaymentProvider implements PaymentProvider {

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.CARD;
    }

    @Override
    public PaymentResult initiatePayment(Order order) {
        String txId = "CARD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String instructions = "Payment of $" + order.getTotalAmount() + " processed securely via Credit / Debit Card.";
        return PaymentResult.success(txId, instructions);
    }
}
