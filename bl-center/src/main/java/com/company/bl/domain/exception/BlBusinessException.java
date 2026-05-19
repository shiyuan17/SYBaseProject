package com.company.bl.domain.exception;

import com.company.common.core.enums.ErrorCode;
import com.company.common.core.exception.BaseException;

public class BlBusinessException extends BaseException {

    public BlBusinessException(ErrorCode errorCode, int httpStatus) {
        super(errorCode, httpStatus);
    }

    public BlBusinessException(ErrorCode errorCode, int httpStatus, String detailMessage) {
        super(errorCode, httpStatus, detailMessage);
    }
}
