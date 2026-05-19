package com.company.bl.domain.enums;

public enum TransportItemStatus {
    PENDING,
    HANDED_OVER,
    PARTIALLY_RECEIVED,
    COMPLETED,
    RETURNED;

    public static TransportItemStatus from(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        return TransportItemStatus.valueOf(value.trim().toUpperCase());
    }
}
