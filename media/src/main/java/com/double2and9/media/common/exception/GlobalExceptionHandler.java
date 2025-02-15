package com.double2and9.media.common.exception;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.enums.MediaErrorCode;
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

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<?> handleValidationException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        
        String errorMessage = fieldErrors.stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("; "));

        log.warn("参数校验失败: {}", errorMessage);
        
        return CommonResponse.error(
            String.valueOf(HttpStatus.BAD_REQUEST.value()),
            errorMessage
        );
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(MediaException.class)
    public CommonResponse<?> handleMediaException(MediaException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return CommonResponse.error(String.valueOf(ex.getCode()), ex.getMessage());
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResponse<?> handleException(Exception ex) {
        log.error("系统异常：", ex);
        return CommonResponse.error(
            String.valueOf(MediaErrorCode.SYSTEM_ERROR.getCode()),
            "系统内部错误"
        );
    }
} 