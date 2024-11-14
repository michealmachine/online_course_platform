package com.double2and9.media.common.exception;

import com.double2and9.base.enums.MediaErrorCode;
import com.double2and9.media.common.model.MediaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MediaException.class)
    public MediaResponse<Void> handleMediaException(MediaException e) {
        log.error("业务异常：{}", e.getMessage());
        return MediaResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public MediaResponse<Void> handleException(Exception e) {
        log.error("系统异常：", e);
        return MediaResponse.error(MediaErrorCode.SYSTEM_ERROR);
    }
} 