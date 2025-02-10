package com.double2and9.content_service.common.model;

import lombok.Data;

@Data
public class ContentResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ContentResponse<T> success(T data) {
        ContentResponse<T> response = new ContentResponse<>();
        response.setCode(0);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ContentResponse<T> success(T data, String message) {
        ContentResponse<T> response = new ContentResponse<>();
        response.setCode(0);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    public static <T> ContentResponse<T> error(int code, String message) {
        ContentResponse<T> response = new ContentResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}