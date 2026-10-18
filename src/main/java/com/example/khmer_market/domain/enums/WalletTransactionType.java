package com.example.khmer_market.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum WalletTransactionType {
    DEPOSIT("deposit"),
    WITHDRAW("withdraw"),
    PAYMENT("payment"),
    EARNING("earning"),
    COMMISSION("commission"),
    FEE("fee"),
    REFUND("refund");

    private final String value;

    WalletTransactionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WalletTransactionType fromValue(String value) {
        if (value == null) return null;
        for (WalletTransactionType type : values()) {
            if (type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown WalletTransactionType: " + value);
    }

    @Converter(autoApply = true)
    public static class WalletTransactionTypeConverter implements AttributeConverter<WalletTransactionType, String> {
        @Override
        public String convertToDatabaseColumn(WalletTransactionType attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public WalletTransactionType convertToEntityAttribute(String dbData) {
            return dbData != null ? WalletTransactionType.fromValue(dbData) : null;
        }
    }
}
