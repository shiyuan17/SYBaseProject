package com.company.bl.domain.enums;

import com.company.common.core.enums.ErrorCode;

public enum BlErrorCode implements ErrorCode {
    INVALID_ARGUMENT("INVALID_ARGUMENT", "Request argument is invalid"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Resource not found"),
    RESOURCE_CONFLICT("RESOURCE_CONFLICT", "Resource conflict"),
    OPERATION_NOT_ALLOWED("OPERATION_NOT_ALLOWED", "Operation is not allowed"),
    NUMBERING_GENERATION_FAILED("NUMBERING_GENERATION_FAILED", "Failed to generate business number");

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
