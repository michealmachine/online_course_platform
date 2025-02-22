package com.double2and9.auth_service.cache;

import com.double2and9.auth_service.dto.response.PermissionTreeNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PermissionCacheManager {
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String PERMISSION_TREE_KEY = "permission:tree";
    private static final String ROLE_PERMISSIONS_KEY = "role:permissions:";
    private static final long CACHE_TTL = 24; // 24小时
    
    public void cachePermissionTree(List<PermissionTreeNode> tree) {
        redisTemplate.opsForValue().set(PERMISSION_TREE_KEY, tree, CACHE_TTL, TimeUnit.HOURS);
    }
    
    @SuppressWarnings("unchecked")
    public List<PermissionTreeNode> getPermissionTree() {
        return (List<PermissionTreeNode>) redisTemplate.opsForValue().get(PERMISSION_TREE_KEY);
    }
    
    public void clearPermissionTree() {
        redisTemplate.delete(PERMISSION_TREE_KEY);
    }
    
    public void clearRolePermissions(Long roleId) {
        redisTemplate.delete(ROLE_PERMISSIONS_KEY + roleId);
    }
    
    public void clearAllCaches() {
        clearPermissionTree();
        // 可以添加其他缓存清理逻辑
    }
} 