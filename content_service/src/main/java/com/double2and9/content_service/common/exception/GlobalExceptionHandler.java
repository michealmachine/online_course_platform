package com.double2and9.content_service.common.exception;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.common.model.ContentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(ContentException.class)
    public ContentResponse<Void> handleContentException(ContentException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ContentResponse.error(e.getErrorCode().getCode(), e.getErrorCode().getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ContentResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败：{}", message);
        return ContentResponse.error(400, message);
    }

    /**
     * 处理参数类型转换异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ContentResponse<Void> handleTypeMismatchException(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "未知类型";
        String message = String.format("参数'%s'类型错误，应为%s类型", paramName, requiredType);
        log.warn("参数类型转换失败：{}, 原始值：{}", message, e.getValue());
        return ContentResponse.error(400, message);
    }

    /**
     * 处理数据完整性异常
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ContentResponse<Void> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
        log.warn("数据完整性异常：{}", e.getMessage());
        return ContentResponse.error(400, "数据不完整或违反约束");
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ContentResponse<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return ContentResponse.error(500, "系统内部错误");
    }
} 