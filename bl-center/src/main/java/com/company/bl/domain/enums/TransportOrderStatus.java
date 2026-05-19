package com.company.bl.domain.enums;

public enum TransportOrderStatus {
    PENDING,
    PRINTED,
    HANDED_OVER,
    PARTIALLY_RECEIVED,
    COMPLETED,
    CANCELLED;

    public static TransportOrderStatus from(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        return TransportOrderStatus.valueOf(value.trim().toUpperCase());
    }
}
