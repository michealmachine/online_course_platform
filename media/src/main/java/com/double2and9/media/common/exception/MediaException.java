package com.double2and9.media.common.exception;

import com.double2and9.base.enums.MediaErrorCode;
import lombok.Getter;

@Getter
public class MediaException extends RuntimeException {
    private final int code;
    private final String message;

    public MediaException(MediaErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }

    public MediaException(MediaErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message;
    }
} 