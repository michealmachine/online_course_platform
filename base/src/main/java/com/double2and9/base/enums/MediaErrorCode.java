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

    // 分片上传相关错误 2004xx
    UPLOAD_SESSION_NOT_FOUND(200401, "上传会话不存在"),
    UPLOAD_SESSION_INVALID_STATUS(200402, "上传会话状态无效"),
    INVALID_CHUNK_INDEX(200403, "分片索引无效"),
    GENERATE_PRESIGNED_URL_FAILED(200404, "生成预签名URL失败"),
    UPLOAD_NOT_COMPLETED(200405, "文件分片未上传完成"),
    COMPLETE_MULTIPART_UPLOAD_FAILED(200406, "完成分片上传失败"),
    MERGE_CHUNKS_FAILED(200407, "合并分片失败"),
    CREATE_MEDIA_FILE_FAILED(200408, "创建媒体文件记录失败"),
    GET_FILE_SIZE_FAILED(200409, "获取文件大小失败"),

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