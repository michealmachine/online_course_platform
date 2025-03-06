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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

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
                 TOKEN_INVALID,
                 TOKEN_UNSUPPORTED,
                 // PKCE相关错误
                 PKCE_REQUIRED,
                 INVALID_CODE_CHALLENGE_METHOD,
                 INVALID_CODE_CHALLENGE,
                 // 账户状态相关错误
                 ACCOUNT_DISABLED,
                 ACCOUNT_LOCKED,
                 ACCOUNT_EXPIRED,
                 CREDENTIALS_EXPIRED -> HttpStatus.BAD_REQUEST;
            
            // 401 - Unauthorized
            case PASSWORD_ERROR,
                 TOKEN_EXPIRED,
                 TOKEN_SIGNATURE_INVALID,
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.PARAMETER_VALIDATION_FAILED.getCode())
                .message("缺少必要的请求参数: " + ex.getParameterName())
                .details(ex.getMessage())
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

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleOAuth2AuthenticationException(OAuth2AuthenticationException ex) {
        log.error("OAuth2认证异常: {}", ex.getMessage());
        
        OAuth2Error error = ex.getError();
        String errorCode = error.getErrorCode();
        String description = error.getDescription();
        
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        
        // 根据OAuth2错误类型设置适当的HTTP状态码
        if (error instanceof BearerTokenError) {
            BearerTokenError bearerError = (BearerTokenError) error;
            httpStatus = HttpStatus.valueOf(bearerError.getHttpStatus().value());
        } else if ("invalid_token".equals(errorCode) || 
                  "invalid_client".equals(errorCode) ||
                  "unauthorized_client".equals(errorCode)) {
            httpStatus = HttpStatus.UNAUTHORIZED;
        } else if ("insufficient_scope".equals(errorCode) ||
                  "access_denied".equals(errorCode)) {
            httpStatus = HttpStatus.FORBIDDEN;
        }
        
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.OAUTH2_ERROR.getCode())
                .message(error.getErrorCode())
                .details(description)
                .build();
        
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        log.debug("资源未找到: {}", ex.getMessage());
        
        // 对于favicon.ico等静态资源，我们可以返回404而不是错误
        if (ex.getMessage().contains("favicon.ico")) {
            // 返回一个空的响应，状态码为404
            return ResponseEntity.notFound().build();
        }
        
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message("请求的资源不存在")
                .details(ex.getMessage())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.debug("找不到处理程序: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.RESOURCE_NOT_FOUND.getCode())
                .message("请求的路径不存在")
                .details(ex.getMessage())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }
    
    @ExceptionHandler(SessionAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleSessionAuthenticationException(SessionAuthenticationException ex) {
        log.warn("会话认证异常: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.SESSION_ERROR.getCode())
                .message("会话认证失败")
                .details(ex.getMessage())
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @ExceptionHandler({InvalidCsrfTokenException.class, MissingCsrfTokenException.class})
    public ResponseEntity<ErrorResponse> handleCsrfException(Exception ex) {
        log.warn("CSRF错误: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.CSRF_ERROR.getCode())
                .message("CSRF令牌错误")
                .details(ex.getMessage())
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * 处理HTTP请求方法不支持的异常
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex) {
        
        log.warn("不支持的HTTP方法: {}", ex.getMessage());
        
        String method = ex.getMethod();
        String[] supportedMethods = ex.getSupportedMethods();
        
        String details = String.format("不支持的HTTP方法 '%s'", method);
        if (supportedMethods != null && supportedMethods.length > 0) {
            details += String.format("，支持的方法有: %s", String.join(", ", supportedMethods));
        }
        
        ErrorResponse response = ErrorResponse.builder()
                .code(AuthErrorCode.METHOD_NOT_ALLOWED.getCode())
                .message("不支持的HTTP请求方法")
                .details(details)
                .build();
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }
} 