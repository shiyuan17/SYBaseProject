package com.company.bl.domain.enums;

public enum ApplicationFormStatus {
    NOT_UPLOADED,
    PENDING,
    UPLOADED,
    ARCHIVED;

    public static ApplicationFormStatus from(String value) {
        if (value == null || value.isBlank()) {
            return NOT_UPLOADED;
        }
        return ApplicationFormStatus.valueOf(value.trim().toUpperCase());
    }
}
