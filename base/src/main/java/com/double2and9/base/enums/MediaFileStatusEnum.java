package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 媒体文件状态枚举
 */
@Getter
public enum MediaFileStatusEnum {
    
    NORMAL("NORMAL", "正常"),
    DELETED("DELETED", "已删除"),
    PROCESSING("PROCESSING", "处理中"),
    PROCESS_SUCCESS("PROCESS_SUCCESS", "处理成功"),
    PROCESS_FAILED("PROCESS_FAILED", "处理失败");

    private final String code;
    private final String desc;

    MediaFileStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MediaFileStatusEnum getByCode(String code) {
        for (MediaFileStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 