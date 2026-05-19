package com.company.bl.domain.enums;

public enum SpecimenStatus {
    REGISTERED,
    FIXING,
    FIXED,
    IN_TRANSIT,
    RECEIVED,
    REJECTED,
    RETURNED;

    public static SpecimenStatus from(String value) {
        if (value == null || value.isBlank()) {
            return REGISTERED;
        }
        return SpecimenStatus.valueOf(value.trim().toUpperCase());
    }
}
