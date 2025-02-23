package com.double2and9.auth_service.exception;

import com.double2and9.auth_service.dto.response.ErrorResponse;
import com.double2and9.base.enums.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        ErrorResponse response = new ErrorResponse(
            ex.getErrorCode().getCode(),
            ex.getErrorCode().getMessage(),
            null
        );
        
        // 根据错误码确定 HTTP 状态码
        HttpStatus status = switch (ex.getErrorCode()) {
            // 404 - Not Found
            case USER_NOT_FOUND, ROLE_NOT_EXISTS, PERMISSION_NOT_FOUND, 
                 CLIENT_NOT_FOUND, AUTHORIZATION_REQUEST_NOT_FOUND -> HttpStatus.NOT_FOUND;
            
            // 400 - Bad Request
            case USERNAME_ALREADY_EXISTS,
                 EMAIL_ALREADY_EXISTS,
                 ROLE_ALREADY_EXISTS,
                 PERMISSION_ALREADY_EXISTS,
                 PERMISSION_IN_USE,
                 ROLE_IN_USE,
                 INVALID_ROLE,
                 CLIENT_ID_EXISTS,
                 PARAMETER_VALIDATION_FAILED,
                 INVALID_APPROVED_SCOPES,
                 CLIENT_REDIRECT_URI_INVALID,
                 CLIENT_SCOPE_INVALID,
                 RESPONSE_TYPE_INVALID,
                 INVALID_GRANT_TYPE,
                 INVALID_AUTHORIZATION_CODE,
                 AUTHORIZATION_CODE_GENERATE_ERROR,
                 // 账户状态相关错误
                 ACCOUNT_DISABLED,
                 ACCOUNT_LOCKED,
                 ACCOUNT_EXPIRED,
                 CREDENTIALS_EXPIRED -> HttpStatus.BAD_REQUEST;
            
            // 401 - Unauthorized
            case PASSWORD_ERROR,
                 TOKEN_EXPIRED,
                 TOKEN_INVALID,
                 TOKEN_SIGNATURE_INVALID,
                 TOKEN_UNSUPPORTED,
                 TOKEN_CLAIMS_EMPTY,
                 TOKEN_REVOKED,
                 INVALID_CLIENT_CREDENTIALS,
                 AUTHENTICATION_FAILED -> HttpStatus.UNAUTHORIZED;
            
            // 403 - Forbidden
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            
            // 500 - Internal Server Error
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.PERMISSION_DENIED.getCode())
                .message(AuthErrorCode.PERMISSION_DENIED.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.PARAMETER_VALIDATION_FAILED.getCode())
                .message(AuthErrorCode.PARAMETER_VALIDATION_FAILED.getMessage())
                .details(details)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.PASSWORD_ERROR.getCode())
                .message(AuthErrorCode.PASSWORD_ERROR.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.SYSTEM_ERROR.getCode())
                .message(AuthErrorCode.SYSTEM_ERROR.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
} 