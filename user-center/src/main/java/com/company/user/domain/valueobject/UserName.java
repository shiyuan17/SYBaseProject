package com.company.user.domain.valueobject;

import com.company.common.core.model.DomainValueObject;
import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;

public record UserName(String value) implements DomainValueObject {

    public UserName(String value) {
        if (value == null || value.isBlank()) {
            throw new UserDomainException(UserErrorCode.INVALID_USER_NAME, 400);
        }
        String normalizedValue = value.trim();
        if (normalizedValue.length() > 64) {
            throw new UserDomainException(UserErrorCode.INVALID_USER_NAME, 400, "用户名长度不能超过64");
        }
        this.value = normalizedValue;
    }
}
