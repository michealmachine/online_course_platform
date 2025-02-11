package com.double2and9.content_service.utils;

import com.double2and9.content_service.common.exception.ContentException;

public class SecurityUtils {
    /**
     * 获取当前登录用户的机构ID
     * TODO: 后续从Token中获取，当前返回测试值
     */
    public static Long getCurrentOrganizationId() {
        // 后续从Token中获取
        return 1234L; // 测试值
    }

    /**
     * 验证是否有权限访问指定机构的数据
     * TODO: 后续基于Token中的信息验证，当前仅做简单比对
     */
    public static boolean hasOrganizationPermission(Long organizationId) {
        return getCurrentOrganizationId().equals(organizationId);
    }

    /**
     * 获取当前用户ID
     * TODO: 后续从Token中获取，当前返回测试值
     */
    public static Long getCurrentUserId() {
        return 999L; // 测试值
    }
}