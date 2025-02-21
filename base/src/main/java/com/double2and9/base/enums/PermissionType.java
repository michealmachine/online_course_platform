package com.double2and9.base.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 权限类型枚举
 */
@Getter
@AllArgsConstructor
public enum PermissionType {
    /**
     * API接口权限
     */
    API("API接口权限"),
    
    /**
     * UI界面权限
     */
    UI("UI界面权限"),
    
    /**
     * OAuth2授权范围
     */
    OAUTH2("OAuth2授权范围");

    private final String description;

    public static PermissionType getByName(String name) {
        for (PermissionType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }
} 