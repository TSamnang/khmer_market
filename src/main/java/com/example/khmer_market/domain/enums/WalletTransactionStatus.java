package com.example.khmer_market.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum WalletTransactionStatus {
    COMPLETED("completed"),
    PENDING("pending"),
    FAILED("failed");

    private final String value;

    WalletTransactionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WalletTransactionStatus fromValue(String value) {
        if (value == null) return null;
        for (WalletTransactionStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown WalletTransactionStatus: " + value);
    }

    @Converter(autoApply = true)
    public static class WalletTransactionStatusConverter implements AttributeConverter<WalletTransactionStatus, String> {
        @Override
        public String convertToDatabaseColumn(WalletTransactionStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public WalletTransactionStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? WalletTransactionStatus.fromValue(dbData) : null;
        }
    }
}
