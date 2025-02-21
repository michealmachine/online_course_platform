package com.double2and9.auth_service.config;

import com.double2and9.auth_service.dto.response.ResourceMeta;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ResourceMetaConfig {
    @Bean("resourceMetaMap")
    public Map<String, ResourceMeta> resourceMetaMap() {
        Map<String, ResourceMeta> metaMap = new HashMap<>();
        metaMap.put("", new ResourceMeta("系统权限", 0));
        metaMap.put("user", new ResourceMeta("用户管理", 1));
        metaMap.put("role", new ResourceMeta("角色管理", 2));
        metaMap.put("permission", new ResourceMeta("权限管理", 3));
        metaMap.put("profile", new ResourceMeta("用户档案", 4));
        metaMap.put("test", new ResourceMeta("测试资源", 99));
        return metaMap;
    }
}

