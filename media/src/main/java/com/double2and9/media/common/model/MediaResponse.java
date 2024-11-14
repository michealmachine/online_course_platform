package com.double2and9.media.common.model;

import com.double2and9.base.enums.MediaErrorCode;
import lombok.Data;

@Data
public class MediaResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> MediaResponse<T> success(T data) {
        MediaResponse<T> response = new MediaResponse<>();
        response.setCode(0);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> MediaResponse<T> error(int code, String message) {
        MediaResponse<T> response = new MediaResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    public static <T> MediaResponse<T> error(MediaErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }
}