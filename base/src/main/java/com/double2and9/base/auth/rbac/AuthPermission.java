package com.double2and9.base.auth.rbac;

import com.double2and9.base.enums.PermissionType;

/**
 * 认证权限接口
 * 所有权限实体类都应实现此接口
 */
public interface AuthPermission {
    /**
     * 获取权限ID
     *
     * @return 权限ID
     */
    Long getId();
    
    /**
     * 获取权限名称
     *
     * @return 权限名称
     */
    String getName();
    
    /**
     * 获取资源标识
     *
     * @return 资源标识
     */
    String getResource();
    
    /**
     * 获取操作类型
     *
     * @return 操作类型
     */
    String getAction();
    
    /**
     * 获取权限类型
     *
     * @return 权限类型
     */
    PermissionType getType();
    
    /**
     * 获取OAuth2授权范围
     * 仅当权限类型为OAUTH2时有意义
     *
     * @return 授权范围
     */
    String getScope();
} 