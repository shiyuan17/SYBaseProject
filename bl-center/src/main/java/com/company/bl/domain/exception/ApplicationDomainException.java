package com.company.bl.domain.exception;

import com.company.bl.domain.enums.ApplicationErrorCode;
import com.company.common.core.exception.BaseException;

public class ApplicationDomainException extends BaseException {

    public ApplicationDomainException(ApplicationErrorCode errorCode, int httpStatus) {
        super(errorCode, httpStatus);
    }

    public ApplicationDomainException(ApplicationErrorCode errorCode, int httpStatus, String detailMessage) {
        super(errorCode, httpStatus, detailMessage);
    }
}
