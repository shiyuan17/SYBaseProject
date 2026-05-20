package com.company.auth.domain.enums;

import com.company.common.core.enums.ErrorCode;

public enum AuthCenterErrorCode implements ErrorCode {
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "Login name or password is incorrect"),
    USER_DISABLED("USER_DISABLED", "Current account is disabled");

    private final String code;
    private final String message;

    AuthCenterErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
