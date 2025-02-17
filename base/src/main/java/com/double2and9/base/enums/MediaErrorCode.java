package com.double2and9.base.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
    INVALID_CHUNK_INDEX(200403, "无效的分片索引"),
    GENERATE_PRESIGNED_URL_FAILED(200404, "生成预签名URL失败"),
    UPLOAD_NOT_COMPLETED(200405, "文件分片未上传完成"),
    COMPLETE_MULTIPART_UPLOAD_FAILED(200406, "完成分片上传失败"),
    MERGE_CHUNKS_FAILED(200407, "合并分片失败"),
    CREATE_MEDIA_FILE_FAILED(200408, "创建媒体文件记录失败"),
    GET_FILE_SIZE_FAILED(200409, "获取文件大小失败"),
    VERIFY_CHUNKS_FAILED(200410, "分片校验失败"),
    ABORT_MULTIPART_UPLOAD_FAILED(200411, "取消分片上传失败"),
    UPLOAD_SESSION_COMPLETED(200408, "上传会话已完成"),
    GET_UPLOAD_STATUS_FAILED(200412, "获取上传状态失败"),

    // 处理相关错误 2002xx
    PROCESS_FAILED(200201, "文件处理失败"),
    PROCESS_STATUS_ERROR(200202, "处理状态错误"),
    PROCESS_TIMEOUT(200203, "处理超时"),
    PROCESS_QUEUE_FULL(200204, "处理队列已满"),

    // MinIO相关错误 2003xx
    MINIO_CONNECTION_ERROR(200301, "MinIO连接失败"),
    MINIO_BUCKET_ERROR(200302, "存储桶操作失败"),
    MINIO_UPLOAD_ERROR(200303, "MinIO上传失败"),
    MINIO_DOWNLOAD_ERROR(200304, "MinIO下载失败"),
    MINIO_DELETE_ERROR(200305, "MinIO删除失败"),
    MINIO_LIST_ERROR(200306, "MinIO列表获取失败"),

    // 权限相关错误 2005xx
    UNAUTHORIZED(200501, "未授权访问"),
    FORBIDDEN(200502, "禁止访问"),
    TOKEN_EXPIRED(200503, "令牌已过期"),
    TOKEN_INVALID(200504, "令牌无效"),

    // 系统错误 2999xx
    PARAM_ERROR(299901, "参数错误"),
    SYSTEM_ERROR(299999, "系统内部错误");

    private final Integer code;
    private final String message;

    public static MediaErrorCode getByCode(int code) {
        for (MediaErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("MediaErrorCode{code=%d, message='%s'}", code, message);
    }
}