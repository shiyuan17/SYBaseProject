package com.company.common.security.exception;

import com.company.common.core.enums.ErrorCode;

public enum SecurityErrorCode implements ErrorCode {
    AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED", "Authentication is required"),
    INVALID_ACCESS_TOKEN("INVALID_ACCESS_TOKEN", "Access token is invalid"),
    ACCESS_TOKEN_EXPIRED("ACCESS_TOKEN_EXPIRED", "Access token is expired"),
    ACCESS_TOKEN_REVOKED("ACCESS_TOKEN_REVOKED", "Access token is revoked");

    private final String code;
    private final String message;

    SecurityErrorCode(String code, String message) {
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
