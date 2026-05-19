package com.company.bl.domain.enums;

import com.company.bl.domain.exception.ApplicationDomainException;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    IN_TRANSIT,
    RECEIVED,
    PARTIALLY_RECEIVED,
    REJECTED,
    CLOSED,
    CANCELLED;

    public static ApplicationStatus from(String value) {
        if (value == null || value.isBlank()) {
            return DRAFT;
        }
        try {
            return ApplicationStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApplicationDomainException(
                ApplicationErrorCode.INVALID_APPLICATION_STATUS,
                400,
                "Unsupported application status: " + value);
        }
    }
}
