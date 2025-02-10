package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 课程审核状态
 */
@Getter
public enum CourseAuditStatusEnum {
    SUBMITTED("202004", "已提交审核"),
    APPROVED("202005", "审核通过"),
    REJECTED("202006", "审核不通过");

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