package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 媒资审核状态
 */
@Getter
public enum MediaAuditStatusEnum {
    
    UNAUDITED("1", "未审核"),
    AUDITING("2", "审核中"),
    APPROVED("3", "审核通过"),
    REJECTED("4", "审核不通过");

    private final String code;
    private final String desc;

    MediaAuditStatusEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static MediaAuditStatusEnum getByCode(String code) {
        for (MediaAuditStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
} 