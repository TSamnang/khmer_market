package com.example.khmer_market.payment.cod;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.payment.PaymentProvider;
import com.example.khmer_market.payment.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CodPaymentProvider implements PaymentProvider {

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.COD;
    }

    @Override
    public PaymentResult initiatePayment(Order order) {
        String txId = "COD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return PaymentResult.success(txId, "Cash on Delivery: Payment will be collected in cash upon package arrival.");
    }
}
