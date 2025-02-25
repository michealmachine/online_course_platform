package com.double2and9.base.auth.rbac;

import java.util.Collection;

/**
 * 认证角色接口
 * 所有角色实体类都应实现此接口
 */
public interface AuthRole {
    /**
     * 获取角色ID
     *
     * @return 角色ID
     */
    Long getId();
    
    /**
     * 获取角色名称
     *
     * @return 角色名称
     */
    String getName();
    
    /**
     * 获取角色拥有的权限
     *
     * @return 权限集合
     */
    Collection<? extends AuthPermission> getPermissions();
} 