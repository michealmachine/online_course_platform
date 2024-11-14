package com.double2and9.base.enums;

import lombok.Getter;

@Getter
public enum MediaErrorCode {
    // 文件相关错误 2001xx
    FILE_NOT_EXISTS(200101, "文件不存在"),
    FILE_UPLOAD_FAILED(200102, "文件上传失败"),
    FILE_TYPE_ERROR(200103, "文件类型错误"),
    FILE_SIZE_ERROR(200104, "文件大小超出限制"),
    FILE_MD5_ERROR(200105, "文件MD5校验失败"),
    FILE_DELETE_FAILED(200106, "文件删除失败"),
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