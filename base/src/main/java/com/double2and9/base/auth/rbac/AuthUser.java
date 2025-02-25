package com.double2and9.base.auth.rbac;

import java.util.Collection;

/**
 * 认证用户接口
 * 所有需要进行权限检查的用户实体类都应实现此接口
 */
public interface AuthUser {
    /**
     * 获取用户ID
     *
     * @return 用户ID
     */
    Long getId();
    
    /**
     * 获取用户名
     *
     * @return 用户名
     */
    String getUsername();
    
    /**
     * 检查用户是否启用
     *
     * @return 是否启用
     */
    boolean isEnabled();
    
    /**
     * 获取用户角色
     *
     * @return 角色集合
     */
    Collection<? extends AuthRole> getRoles();
} 