package com.salemale.common.exception.handler;

import com.salemale.common.code.BaseErrorCode;
import com.salemale.common.exception.GeneralException;

public class BaseHandler extends GeneralException {
    public BaseHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}