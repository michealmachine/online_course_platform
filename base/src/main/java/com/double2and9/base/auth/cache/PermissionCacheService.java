package com.double2and9.base.auth.cache;

import java.util.List;

/**
 * 权限缓存服务接口
 */
public interface PermissionCacheService {
    /**
     * 缓存权限树
     *
     * @param tree 权限树
     */
    void cachePermissionTree(List<?> tree);
    
    /**
     * 获取缓存的权限树
     *
     * @return 权限树
     */
    List<?> getPermissionTree();
    
    /**
     * 清除权限树缓存
     */
    void clearPermissionTree();
    
    /**
     * 清除角色权限缓存
     *
     * @param roleId 角色ID
     */
    void clearRolePermissions(Long roleId);
    
    /**
     * 清除所有权限相关缓存
     */
    void clearAllCaches();
} 