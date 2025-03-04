package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.AssignPermissionRequest;
import com.double2and9.auth_service.entity.Permission;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.repository.PermissionRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.base.enums.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import com.double2and9.auth_service.cache.PermissionCacheManager;

import java.util.HashSet;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoleControllerIntegrationTest extends BaseOAuth2IntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionCacheManager permissionCacheManager;

    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        
        permissionCacheManager.clearPermissionTree();
        // 创建测试角色
        testRole = new Role();
        testRole.setName("ROLE_TEST");
        testRole.setDescription("Test Role");
        testRole.setPermissions(new HashSet<>());
        testRole = roleRepository.save(testRole);

        // 创建测试权限
        testPermission = new Permission();
        testPermission.setName("test:read");
        testPermission.setResource("test");
        testPermission.setAction("read");
        testPermission.setDescription("Test Permission");
        testPermission = permissionRepository.save(testPermission);
    }

    @AfterEach
    public void tearDown() {
        permissionCacheManager.clearPermissionTree();
    }

    @Test
    void assignPermissions_Success() throws Exception {
        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(testPermission.getId()));

        mockMvc.perform(post("/api/roles/{roleId}/permissions", testRole.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void assignPermissions_RoleNotFound() throws Exception {
        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(testPermission.getId()));

        mockMvc.perform(post("/api/roles/{roleId}/permissions", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.ROLE_NOT_EXISTS.getCode()));
    }

    @Test
    void assignPermissions_NoPermission() throws Exception {
        // 使用普通用户token
        setupUserWithToken();

        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(testPermission.getId()));

        mockMvc.perform(post("/api/roles/{roleId}/permissions", testRole.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRolePermissions_Success() throws Exception {
        mockMvc.perform(get("/api/roles/{roleId}/permissions", testRole.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(testRole.getId()))
                .andExpect(jsonPath("$.roleName").value(testRole.getName()))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    void revokePermission_Success() throws Exception {
        // 先分配权限
        testRole.getPermissions().add(testPermission);
        roleRepository.save(testRole);

        mockMvc.perform(delete("/api/roles/{roleId}/permissions/{permissionId}",
                testRole.getId(), testPermission.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void assignPermissions_ShouldClearCache() throws Exception {
        // 先获取权限树并缓存
        mockMvc.perform(get("/api/permissions/tree")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 分配权限
        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(testPermission.getId()));

        mockMvc.perform(post("/api/roles/{roleId}/permissions", testRole.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 验证缓存已被清除
        assertNull(permissionCacheManager.getPermissionTree());
    }
} 