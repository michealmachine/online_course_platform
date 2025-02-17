package com.double2and9.base.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 上传状态枚举
 */
@Getter
@AllArgsConstructor
public enum UploadStatusEnum {
    
    UPLOADING("UPLOADING", "上传中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "上传失败"),
    ABORTED("ABORTED", "已取消");

    private final String code;
    private final String desc;

    public static UploadStatusEnum getByCode(String code) {
        for (UploadStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 