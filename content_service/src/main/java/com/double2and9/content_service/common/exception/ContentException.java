package com.double2and9.content_service.common.exception;

import com.double2and9.base.enums.ContentErrorCode;
import lombok.Getter;

@Getter
public class ContentException extends RuntimeException {
    private final int code;
    private final String message;

    public ContentException(ContentErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public ContentException(ContentErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message;
    }
} 