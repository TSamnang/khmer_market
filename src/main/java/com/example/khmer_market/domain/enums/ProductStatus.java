package com.example.khmer_market.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ProductStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ProductStatus fromValue(String value) {
        if (value == null) return null;
        for (ProductStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ProductStatus value: " + value);
    }

    @Converter(autoApply = true)
    public static class ProductStatusConverter implements AttributeConverter<ProductStatus, String> {
        @Override
        public String convertToDatabaseColumn(ProductStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ProductStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? ProductStatus.fromValue(dbData) : null;
        }
    }
}
