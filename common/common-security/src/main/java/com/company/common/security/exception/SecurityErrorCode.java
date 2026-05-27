package com.company.common.security.exception;

import com.company.common.core.enums.ErrorCode;

public enum SecurityErrorCode implements ErrorCode {
    AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED", "需要登录认证"),
    INVALID_ACCESS_TOKEN("INVALID_ACCESS_TOKEN", "访问令牌无效"),
    ACCESS_TOKEN_EXPIRED("ACCESS_TOKEN_EXPIRED", "访问令牌已过期"),
    ACCESS_TOKEN_REVOKED("ACCESS_TOKEN_REVOKED", "访问令牌已失效");

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
