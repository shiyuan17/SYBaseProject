package com.company.bl.domain.enums;

import com.company.common.core.enums.ErrorCode;

public enum BlErrorCode implements ErrorCode {
    INVALID_ARGUMENT("INVALID_ARGUMENT", "请求参数无效"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在"),
    RESOURCE_CONFLICT("RESOURCE_CONFLICT", "资源冲突"),
    OPERATION_NOT_ALLOWED("OPERATION_NOT_ALLOWED", "当前操作不被允许"),
    AUTHENTICATION_REQUIRED("AUTHENTICATION_REQUIRED", "需要登录认证"),
    PERMISSION_DENIED("PERMISSION_DENIED", "没有权限执行该操作"),
    NUMBERING_GENERATION_FAILED("NUMBERING_GENERATION_FAILED", "业务编号生成失败"),
    EXTERNAL_INTEGRATION_UNAVAILABLE("EXTERNAL_INTEGRATION_UNAVAILABLE", "外部集成不可用");

    private final String code;
    private final String message;

    BlErrorCode(String code, String message) {
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
