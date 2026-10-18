package com.example.khmer_market.payment.bank;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.payment.PaymentProvider;
import com.example.khmer_market.payment.PaymentResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BankTransferPaymentProvider implements PaymentProvider {

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.BANK_TRANSFER;
    }

    @Override
    public PaymentResult initiatePayment(Order order) {
        String txId = "BANK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String instructions = "Please transfer " + order.getTotalAmount() + " USD to:\n" +
                "Bank: ABA Bank Cambodia\n" +
                "Account Name: KHMER MARKETPLACE PLATFORM\n" +
                "Account Number: 001 234 567 (USD)\n" +
                "Reference Code: " + order.getOrderNumber() + "\n" +
                "Our administrative team will verify your transfer and confirm your order.";
        return PaymentResult.success(txId, instructions);
    }
}
