package com.company.user.domain.valueobject;

import com.company.common.core.model.DomainValueObject;
import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;

public record UserId(String value) implements DomainValueObject {

    public UserId(String value) {
        if (value == null || value.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_USER_ID, 400);
        }
        this.value = value;
    }
}
