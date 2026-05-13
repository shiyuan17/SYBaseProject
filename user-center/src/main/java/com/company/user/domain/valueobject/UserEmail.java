package com.company.user.domain.valueobject;

import com.company.common.core.model.DomainValueObject;
import com.company.user.domain.enums.UserErrorCode;
import com.company.user.domain.exception.UserDomainException;

import java.util.regex.Pattern;

public record UserEmail(String value) implements DomainValueObject {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public UserEmail(String value) {
        if (value == null || value.isBlank() || !EMAIL_PATTERN.matcher(value).matches()) {
            throw new UserDomainException(UserErrorCode.INVALID_USER_EMAIL, 400);
        }
        this.value = value.trim().toLowerCase();
    }
}
