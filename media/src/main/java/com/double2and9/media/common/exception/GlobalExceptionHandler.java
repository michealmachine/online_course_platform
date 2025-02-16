package com.double2and9.media.common.exception;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.enums.MediaErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
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
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<?> handleValidationException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) e).getBindingResult()
                .getFieldError().getDefaultMessage();
        } else {
            message = ((BindException) e).getBindingResult()
                .getFieldError().getDefaultMessage();
        }
        log.error("参数校验失败: message={}", message);
        return CommonResponse.error(String.valueOf(HttpStatus.BAD_REQUEST.value()), message);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(MediaException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResponse<?> handleMediaException(MediaException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return CommonResponse.error(String.valueOf(e.getCode()), e.getMessage());
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResponse<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return CommonResponse.error(
            String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), 
            "系统内部错误"
        );
    }
} 