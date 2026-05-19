package com.company.common.core.exception;

import com.company.common.core.enums.ErrorCode;
import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final int httpStatus;

    protected BaseException(ErrorCode errorCode, int httpStatus) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected BaseException(ErrorCode errorCode, int httpStatus, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
