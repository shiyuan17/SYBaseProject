package com.company.bl.domain.enums;

public enum FixationStatus {
    PENDING,
    FIXING,
    COMPLETED,
    ABNORMAL;

    public static FixationStatus from(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        return FixationStatus.valueOf(value.trim().toUpperCase());
    }
}
