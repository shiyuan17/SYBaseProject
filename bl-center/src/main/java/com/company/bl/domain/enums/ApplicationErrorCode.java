package com.company.bl.domain.enums;

import com.company.common.core.enums.ErrorCode;

public enum ApplicationErrorCode implements ErrorCode {
    APPLICATION_NOT_FOUND("APPLICATION_NOT_FOUND", "申请单不存在"),
    APPLICATION_NO_CONFLICT("APPLICATION_NO_CONFLICT", "申请单号已存在"),
    INVALID_APPLICATION_ID("INVALID_APPLICATION_ID", "申请单ID无效"),
    INVALID_APPLICATION_NO("INVALID_APPLICATION_NO", "申请单号无效"),
    INVALID_APPLICATION_FIELD("INVALID_APPLICATION_FIELD", "申请单字段无效"),
    INVALID_APPLICATION_STATUS("INVALID_APPLICATION_STATUS", "申请单状态无效");

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
