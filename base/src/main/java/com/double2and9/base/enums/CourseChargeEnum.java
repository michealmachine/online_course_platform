package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 课程收费规则
 */
@Getter
public enum CourseChargeEnum {
    
    FREE("201001", "免费"),
    CHARGE("201002", "收费");

    private final String code;
    private final String desc;

    CourseChargeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CourseChargeEnum getByCode(String code) {
        for (CourseChargeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 