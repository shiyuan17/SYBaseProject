package com.company.bl.domain.enums;

public enum ReceiptStatus {
    RECEIVED,
    REJECTED,
    RETURNED;

    public static ReceiptStatus from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Receipt status is required");
        }
        return ReceiptStatus.valueOf(value.trim().toUpperCase());
    }
}
