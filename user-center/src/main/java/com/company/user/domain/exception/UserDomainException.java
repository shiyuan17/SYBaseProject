package com.company.user.domain.exception;

import com.company.common.core.exception.BaseException;
import com.company.user.domain.enums.UserErrorCode;

public class UserDomainException extends BaseException {

    public UserDomainException(UserErrorCode errorCode, int httpStatus) {
        super(errorCode, httpStatus);
    }

    public UserDomainException(UserErrorCode errorCode, int httpStatus, String detailMessage) {
        super(errorCode, httpStatus, detailMessage);
    }
}
