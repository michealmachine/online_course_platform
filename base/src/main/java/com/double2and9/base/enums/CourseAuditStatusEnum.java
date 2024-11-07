package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 课程审核状态
 */
@Getter
public enum CourseAuditStatusEnum {
    
    SUBMITTED("202301", "已提交"),
    AUDITING("202302", "审核中"),
    APPROVED("202303", "审核通过"),
    REJECTED("202304", "审核不通过");

    private final String code;
    private final String desc;

    CourseAuditStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CourseAuditStatusEnum getByCode(String code) {
        for (CourseAuditStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 