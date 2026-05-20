package com.company.common.core.enums;

public enum CommonErrorCode implements ErrorCode {
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error"),
    VALIDATION_ERROR("VALIDATION_ERROR", "Request validation failed"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found"),
    CONFLICT("CONFLICT", "Resource state conflict");

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
