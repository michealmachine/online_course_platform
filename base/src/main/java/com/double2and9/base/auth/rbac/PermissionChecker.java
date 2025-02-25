package com.double2and9.base.auth.rbac;

import com.double2and9.base.enums.PermissionType;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限检查工具类
 */
@Slf4j
public class PermissionChecker {
    
    /**
     * 检查用户是否拥有指定资源和操作的权限
     *
     * @param user 用户
     * @param resource 资源标识
     * @param action 操作类型
     * @return 是否拥有权限
     */
    public static boolean hasPermission(AuthUser user, String resource, String action) {
        if (user == null || !user.isEnabled()) {
            return false;
        }
        
        // 从用户角色中查找权限
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(AuthRole::getPermissions)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .anyMatch(permission -> 
                    resource.equals(permission.getResource()) && 
                    action.equals(permission.getAction())
                );
    }
    
    /**
     * 检查用户是否拥有指定角色
     *
     * @param user 用户
     * @param roleName 角色名称
     * @return 是否拥有角色
     */
    public static boolean hasRole(AuthUser user, String roleName) {
        if (user == null || !user.isEnabled()) {
            return false;
        }
        
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .anyMatch(role -> roleName.equals(role.getName()));
    }
    
    /**
     * 获取用户的OAuth2授权范围
     *
     * @param user 用户
     * @return 授权范围集合
     */
    public static Set<String> getPermissionScopes(AuthUser user) {
        if (user == null || !user.isEnabled()) {
            return Collections.emptySet();
        }
        
        return user.getRoles().stream()
                .filter(Objects::nonNull)
                .map(AuthRole::getPermissions)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(permission -> PermissionType.OAUTH2.equals(permission.getType()))
                .map(AuthPermission::getScope)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    /**
     * 检查用户是否拥有指定的OAuth2授权范围
     *
     * @param user 用户
     * @param scope 授权范围
     * @return 是否拥有授权范围
     */
    public static boolean hasPermissionScope(AuthUser user, String scope) {
        return getPermissionScopes(user).contains(scope);
    }
} 