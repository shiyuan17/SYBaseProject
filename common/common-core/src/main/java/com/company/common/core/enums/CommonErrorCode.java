package com.company.common.core.enums;

public enum CommonErrorCode implements ErrorCode {
    INTERNAL_ERROR("INTERNAL_ERROR", "系统内部错误"),
    VALIDATION_ERROR("VALIDATION_ERROR", "请求参数校验失败"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "资源不存在"),
    CONFLICT("CONFLICT", "资源状态冲突");

    private final String code;
    private final String message;

    CommonErrorCode(String code, String message) {
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
