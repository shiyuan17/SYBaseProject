package com.company.bl.domain.enums;

public enum SpecimenStatus {
    REGISTERED,
    VERIFIED,
    FIXING,
    FIXED,
    CHECKED_IN,
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
