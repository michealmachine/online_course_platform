package com.double2and9.auth_service.exception;

import com.double2and9.base.enums.AuthErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthException extends RuntimeException {
    private final AuthErrorCode errorCode;
    private final HttpStatus status;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;  // 默认状态码
    }

    public AuthException(AuthErrorCode errorCode, HttpStatus status) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.status = status;
    }
} 