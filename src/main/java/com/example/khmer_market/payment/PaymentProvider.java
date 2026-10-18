package com.example.khmer_market.payment;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.PaymentMethod;

public interface PaymentProvider {
    PaymentMethod getMethod();
    PaymentResult initiatePayment(Order order);
}
