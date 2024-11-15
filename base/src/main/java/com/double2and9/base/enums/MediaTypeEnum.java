package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 媒资文件类型
 */
@Getter
public enum MediaTypeEnum {
    
    VIDEO("001", "视频"),
    IMAGE("002", "图片"),
    DOC("003", "文档"),
    OTHER("999", "其他");

    private final String code;
    private final String desc;

    MediaTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MediaTypeEnum getByCode(String code) {
        for (MediaTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 