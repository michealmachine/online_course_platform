package com.double2and9.base.auth.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisPermissionCacheManagerTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisPermissionCacheManager cacheManager;

    @Test
    void shouldCachePermissionTree() {
        // 准备测试数据
        List<Object> tree = new ArrayList<>();
        tree.add("test");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // 执行测试
        cacheManager.cachePermissionTree(tree);
        
        // 验证
        verify(valueOperations).set(eq("permission:tree"), eq(tree), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void shouldGetPermissionTree() {
        // 准备测试数据
        List<Object> tree = new ArrayList<>();
        tree.add("test");
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("permission:tree")).thenReturn(tree);
        
        // 执行测试
        List<?> result = cacheManager.getPermissionTree();
        
        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("test", result.get(0));
    }

    @Test
    void shouldReturnNullWhenTreeNotCached() {
        // 准备测试数据
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("permission:tree")).thenReturn(null);
        
        // 执行测试
        List<?> result = cacheManager.getPermissionTree();
        
        // 验证
        assertNull(result);
    }

    @Test
    void shouldClearPermissionTree() {
        // 执行测试
        cacheManager.clearPermissionTree();
        
        // 验证
        verify(redisTemplate).delete("permission:tree");
    }

    @Test
    void shouldClearRolePermissions() {
        // 执行测试
        Long roleId = 1L;
        cacheManager.clearRolePermissions(roleId);
        
        // 验证
        verify(redisTemplate).delete("role:permissions:1");
    }
    
    @Test
    void shouldClearAllCaches() {
        // 准备测试
        String key1 = "permission:tree";
        
        // 执行测试
        cacheManager.clearAllCaches();
        
        // 验证
        verify(redisTemplate).delete(key1);
    }
} 