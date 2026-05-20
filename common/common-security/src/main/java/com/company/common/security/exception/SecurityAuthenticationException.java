package com.company.common.security.exception;

import com.company.common.core.enums.ErrorCode;
import com.company.common.core.exception.BaseException;

public class SecurityAuthenticationException extends BaseException {

    public SecurityAuthenticationException(ErrorCode errorCode, int httpStatus, String detailMessage) {
        super(errorCode, httpStatus, detailMessage);
    }
}
