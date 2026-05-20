package com.company.auth.domain.exception;

import com.company.common.core.enums.ErrorCode;
import com.company.common.core.exception.BaseException;

public class AuthCenterException extends BaseException {

    public AuthCenterException(ErrorCode errorCode, int httpStatus, String detailMessage) {
        super(errorCode, httpStatus, detailMessage);
    }
}
