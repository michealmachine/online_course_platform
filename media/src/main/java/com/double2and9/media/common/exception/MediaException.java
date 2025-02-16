package com.double2and9.media.common.exception;

import com.double2and9.base.enums.MediaErrorCode;
import lombok.Getter;

@Getter
public class MediaException extends RuntimeException {
    private final int code;
    private final MediaErrorCode errorCode;

    public MediaException(MediaErrorCode errorCode) {
        super(errorCode.getMessage());
        if (errorCode == null) {
            throw new IllegalArgumentException("ErrorCode cannot be null");
        }
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public MediaException(MediaErrorCode errorCode, String message) {
        super(message);
        if (errorCode == null) {
            throw new IllegalArgumentException("ErrorCode cannot be null");
        }
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public MediaException(MediaErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.errorCode = errorCode;
    }

    public MediaException(int code, String message) {
        super(message);
        this.code = code;
        this.errorCode = null;
    }

    public MediaException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.errorCode = null;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }

    public MediaErrorCode getErrorCode() {
        return errorCode;
    }
}