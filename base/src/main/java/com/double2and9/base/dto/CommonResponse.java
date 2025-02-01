package com.double2and9.base.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommonResponse<T> {
    private String code;
    private String message;
    private T data;
    private boolean success;

    private CommonResponse(String code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    public static <T> CommonResponse<T> success(T data) {
        return new CommonResponse<>("200", "success", data, true);
    }

    public static <T> CommonResponse<T> success(T data, String message) {
        return new CommonResponse<>("200", message, data, true);
    }

    public static <T> CommonResponse<T> error(String code, String message) {
        return new CommonResponse<>(code, message, null, false);
    }

    public static <T> CommonResponse<T> error(String code, String message, T data) {
        return new CommonResponse<>(code, message, data, false);
    }
} 