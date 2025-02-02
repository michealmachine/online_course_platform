package com.double2and9.media.common.exception;

import com.double2and9.base.enums.MediaErrorCode;
import lombok.Getter;

@Getter
public class MediaException extends RuntimeException {
    private final int code;
    private final String message;

    private final MediaErrorCode errorCode;

    public MediaException(MediaErrorCode errorCode) {
        if (errorCode == null) {
            throw new IllegalArgumentException("ErrorCode cannot be null");
        }
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.errorCode = errorCode;
    }

    public MediaException(MediaErrorCode errorCode, String message) {
        if (errorCode == null) {
            throw new IllegalArgumentException("ErrorCode cannot be null");
        }
        this.code = errorCode.getCode();
        this.message = message;
        this.errorCode = errorCode;
    }

    public MediaErrorCode getErrorCode() {
        return errorCode;
    }
}