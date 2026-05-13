package com.company.user.domain.enums;

import com.company.common.core.enums.ErrorCode;

public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("USER_NOT_FOUND", "用户不存在"),
    USER_EMAIL_CONFLICT("USER_EMAIL_CONFLICT", "邮箱已存在"),
    INVALID_USER_ID("INVALID_USER_ID", "用户标识不正确"),
    INVALID_USER_NAME("INVALID_USER_NAME", "用户名不能为空"),
    INVALID_USER_EMAIL("INVALID_USER_EMAIL", "邮箱格式不正确");

    private final String code;
    private final String message;

    UserErrorCode(String code, String message) {
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
