package com.double2and9.base.enums;

import lombok.Getter;

@Getter
public enum MediaErrorCode {
    // 媒体文件相关错误 2001xx
    FILE_NOT_EXISTS(200101, "文件不存在"),
    MEDIA_TYPE_NOT_SUPPORT(200102, "不支持的媒体类型"),
    UPLOAD_ERROR(200103, "上传失败"),
    DELETE_ERROR(200104, "删除失败"),
    FILE_TOO_LARGE(200105, "文件大小超过限制"),
    FILE_TOO_SMALL(200106, "文件大小过小"),
    FILE_EMPTY(200107, "文件为空"),
    FILE_TYPE_ERROR(200108, "文件类型错误"),
    FILE_UPLOAD_FAILED(200109, "文件上传失败"),

    // 处理相关错误 2002xx
    PROCESS_FAILED(200201, "文件处理失败"),
    PROCESS_STATUS_ERROR(200202, "处理状态错误"),

    // MinIO相关错误 2003xx
    MINIO_CONNECTION_ERROR(200301, "MinIO连接失败"),
    MINIO_BUCKET_ERROR(200302, "存储桶操作失败"),
    MINIO_UPLOAD_ERROR(200303, "MinIO上传失败"),

    // 系统错误 2999xx
    PARAM_ERROR(299901, "参数错误"),
    SYSTEM_ERROR(299999, "系统内部错误");

    private final int code;
    private final String message;

    MediaErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}