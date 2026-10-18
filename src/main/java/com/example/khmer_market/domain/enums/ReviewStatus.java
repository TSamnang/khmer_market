package com.example.khmer_market.domain.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum ReviewStatus {
    PUBLISHED("published"),
    HIDDEN("hidden");

    private final String value;

    ReviewStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ReviewStatus fromValue(String value) {
        if (value == null) return null;
        for (ReviewStatus status : values()) {
            if (status.value.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ReviewStatus value: " + value);
    }

    @Converter(autoApply = true)
    public static class ReviewStatusConverter implements AttributeConverter<ReviewStatus, String> {
        @Override
        public String convertToDatabaseColumn(ReviewStatus attribute) {
            return attribute != null ? attribute.getValue() : null;
        }

        @Override
        public ReviewStatus convertToEntityAttribute(String dbData) {
            return dbData != null ? ReviewStatus.fromValue(dbData) : null;
        }
    }
}
