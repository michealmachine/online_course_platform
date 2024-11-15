package com.double2and9.content_service.common.exception;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.common.model.ContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ContentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ContentResponse<Void> handleContentException(ContentException e) {
        log.error("业务异常：{}", e.getMessage());
        return ContentResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ContentResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        String message = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.error("参数校验失败：{}", message);
        return ContentResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ContentResponse<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return ContentResponse.error(
            ContentErrorCode.SYSTEM_ERROR.getCode(),
            ContentErrorCode.SYSTEM_ERROR.getMessage()
        );
    }
} 