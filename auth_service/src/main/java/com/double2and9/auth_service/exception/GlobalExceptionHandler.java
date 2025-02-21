package com.double2and9.auth_service.exception;

import com.double2and9.auth_service.dto.response.ErrorResponse;
import com.double2and9.base.enums.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException ex) {
        ErrorResponse response = new ErrorResponse(
            ex.getErrorCode().getCode(),
            ex.getErrorCode().getMessage(),
            null
        );
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
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