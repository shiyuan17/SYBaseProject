package com.company.auth.domain.enums;

import com.company.common.core.enums.ErrorCode;

public enum AuthCenterErrorCode implements ErrorCode {
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", "登录名或密码错误"),
    USER_DISABLED("USER_DISABLED", "当前账号已被禁用");

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
