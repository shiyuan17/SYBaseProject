package com.company.bl.domain.enums;

import com.company.common.core.enums.ErrorCode;

public enum ApplicationErrorCode implements ErrorCode {
    APPLICATION_NOT_FOUND("APPLICATION_NOT_FOUND", "Application not found"),
    APPLICATION_NO_CONFLICT("APPLICATION_NO_CONFLICT", "Application number already exists"),
    INVALID_APPLICATION_ID("INVALID_APPLICATION_ID", "Application id is invalid"),
    INVALID_APPLICATION_NO("INVALID_APPLICATION_NO", "Application number is invalid"),
    INVALID_APPLICATION_STATUS("INVALID_APPLICATION_STATUS", "Application status is invalid");

    private final String code;
    private final String message;

    ApplicationErrorCode(String code, String message) {
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
