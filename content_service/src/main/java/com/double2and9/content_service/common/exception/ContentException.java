package com.double2and9.content_service.common.exception;

import com.double2and9.base.enums.ContentErrorCode;
import lombok.Getter;

@Getter
public class ContentException extends RuntimeException {
    private final ContentErrorCode errorCode;

    public ContentException(ContentErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ContentException(ContentErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ContentException(ContentErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
} 