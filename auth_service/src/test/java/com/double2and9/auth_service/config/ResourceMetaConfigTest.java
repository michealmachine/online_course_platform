package com.double2and9.auth_service.config;

import com.double2and9.auth_service.dto.response.ResourceMeta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ResourceMetaConfigTest {

    @Autowired
    private Map<String, ResourceMeta> resourceMetaMap;

    @Test
    void resourceMetaMap_Configuration() {
        assertNotNull(resourceMetaMap);
        
        // 验证用户管理资源
        ResourceMeta userMeta = resourceMetaMap.get("user");
        assertNotNull(userMeta);
        assertEquals("用户管理", userMeta.getDescription());
        assertEquals(1, userMeta.getSort());
        
        // 验证角色管理资源
        ResourceMeta roleMeta = resourceMetaMap.get("role");
        assertNotNull(roleMeta);
        assertEquals("角色管理", roleMeta.getDescription());
        assertEquals(2, roleMeta.getSort());
        
        // 验证权限管理资源
        ResourceMeta permissionMeta = resourceMetaMap.get("permission");
        assertNotNull(permissionMeta);
        assertEquals("权限管理", permissionMeta.getDescription());
        assertEquals(3, permissionMeta.getSort());
    }
} 