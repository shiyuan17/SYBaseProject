package com.company.bl.domain.valueobject;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.bl.domain.exception.ApplicationDomainException;

public record ApplicationId(String value) {

    public ApplicationId {
        if (value == null || value.isBlank()) {
            throw new ApplicationDomainException(ApplicationErrorCode.INVALID_APPLICATION_ID, 400);
        }
    }
}
