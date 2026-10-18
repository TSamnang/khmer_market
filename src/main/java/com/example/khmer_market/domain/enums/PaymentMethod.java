package com.example.khmer_market.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum PaymentMethod {
    COD("cod"),
    KHQR("khqr"),
    BANK_TRANSFER("bank_transfer"),
    CARD("card"),
    BALANCE("balance");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return switch (this) {
            case BALANCE -> "Account Balance";
            case CARD -> "Credit / Debit Card";
            case KHQR -> "Bakong KHQR";
            case COD -> "Cash on Delivery";
            case BANK_TRANSFER -> "Bank Transfer";
        };
    }

    public static PaymentMethod fromValue(String value) {
        if (value == null) return null;
        for (PaymentMethod method : values()) {
            if (method.value.equalsIgnoreCase(value) || method.name().equalsIgnoreCase(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown PaymentMethod value: " + value);
    }

    @Converter(autoApply = true)
    public static class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {
        @Override
        public String convertToDatabaseColumn(PaymentMethod attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public PaymentMethod convertToEntityAttribute(String dbData) {
            return dbData != null ? PaymentMethod.fromValue(dbData) : null;
        }
    }
}
