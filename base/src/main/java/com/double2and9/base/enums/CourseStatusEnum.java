package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 课程状态
 */
@Getter
public enum CourseStatusEnum {
    
    DRAFT("202001", "未发布"),
    PUBLISHED("202002", "已发布"),
    OFFLINE("202003", "已下线");

    private final String code;
    private final String desc;

    CourseStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CourseStatusEnum getByCode(String code) {
        for (CourseStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 