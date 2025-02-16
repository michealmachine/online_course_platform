package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 上传状态枚举
 */
@Getter
public enum UploadStatusEnum {
    
    UPLOADING("UPLOADING", "上传中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "上传失败"),
    ABORTED("ABORTED", "已取消");

    private final String code;
    private final String desc;

    UploadStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UploadStatusEnum getByCode(String code) {
        for (UploadStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 