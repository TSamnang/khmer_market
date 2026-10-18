package com.example.khmer_market.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum PackageStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String value;

    PackageStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PackageStatus fromValue(String value) {
        if (value == null) return null;
        for (PackageStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown PackageStatus value: " + value);
    }

    @Converter(autoApply = true)
    public static class PackageStatusConverter implements AttributeConverter<PackageStatus, String> {
        @Override
        public String convertToDatabaseColumn(PackageStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public PackageStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? PackageStatus.fromValue(dbData) : null;
        }
    }
}
