package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 媒资文件状态
 */
@Getter
public enum MediaStatusEnum {
    
    UPLOADING("1", "上传中"),
    UPLOADED("2", "上传完成"),
    UPLOAD_FAILED("3", "上传失败"),
    PROCESSING("4", "处理中"),
    PROCESS_SUCCESS("5", "处理成功"),
    PROCESS_FAILED("6", "处理失败");

    private final String code;
    private final String desc;

    MediaStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MediaStatusEnum getByCode(String code) {
        for (MediaStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 