package com.double2and9.base.auth.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的权限缓存管理器
 */
@RequiredArgsConstructor
public class RedisPermissionCacheManager implements PermissionCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String PERMISSION_TREE_KEY = "permission:tree";
    private static final String ROLE_PERMISSIONS_KEY = "role:permissions:";
    private static final long CACHE_TTL = 24; // 24小时
    
    @Override
    public void cachePermissionTree(List<?> tree) {
        redisTemplate.opsForValue().set(PERMISSION_TREE_KEY, tree, CACHE_TTL, TimeUnit.HOURS);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<?> getPermissionTree() {
        return (List<?>) redisTemplate.opsForValue().get(PERMISSION_TREE_KEY);
    }
    
    @Override
    public void clearPermissionTree() {
        redisTemplate.delete(PERMISSION_TREE_KEY);
    }
    
    @Override
    public void clearRolePermissions(Long roleId) {
        redisTemplate.delete(ROLE_PERMISSIONS_KEY + roleId);
    }
    
    @Override
    public void clearAllCaches() {
        clearPermissionTree();
        // 可以添加清除更多缓存的逻辑
    }
} 