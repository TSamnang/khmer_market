package com.example.khmer_market.payment.balance;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.payment.PaymentProvider;
import com.example.khmer_market.payment.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BalancePaymentProvider implements PaymentProvider {

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.BALANCE;
    }

    @Override
    public PaymentResult initiatePayment(Order order) {
        String txId = "BAL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String instructions = "Payment of $" + order.getTotalAmount() + " processed instantly from your account balance.";
        return PaymentResult.success(txId, instructions);
    }
}
