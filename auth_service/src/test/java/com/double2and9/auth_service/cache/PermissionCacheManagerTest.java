package com.double2and9.auth_service.cache;

import com.double2and9.auth_service.config.BaseIntegrationTestConfig;
import com.double2and9.auth_service.dto.response.PermissionTreeNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = BaseIntegrationTestConfig.class)
@ActiveProfiles("dev")
class PermissionCacheManagerTest {

    @Autowired
    private PermissionCacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void cacheAndGetPermissionTree() {
        // 准备测试数据
        List<PermissionTreeNode> tree = new ArrayList<>();
        PermissionTreeNode node = new PermissionTreeNode();
        node.setResource("test");
        node.setDescription("Test Resource");
        tree.add(node);

        // 缓存数据
        cacheManager.cachePermissionTree(tree);

        // 获取缓存数据
        List<PermissionTreeNode> cachedTree = cacheManager.getPermissionTree();

        // 验证
        assertNotNull(cachedTree);
        assertEquals(1, cachedTree.size());
        assertEquals("test", cachedTree.get(0).getResource());
    }

    @Test
    void clearPermissionTree() {
        // 准备测试数据
        List<PermissionTreeNode> tree = new ArrayList<>();
        cacheManager.cachePermissionTree(tree);

        // 清除缓存
        cacheManager.clearPermissionTree();

        // 验证
        assertNull(cacheManager.getPermissionTree());
    }

    @Test
    void clearRolePermissions() {
        Long roleId = 1L;
        String key = "role:permissions:" + roleId;

        // 准备测试数据
        redisTemplate.opsForValue().set(key, "test");

        // 清除缓存
        cacheManager.clearRolePermissions(roleId);

        // 验证
        assertNull(redisTemplate.opsForValue().get(key));
    }
} 