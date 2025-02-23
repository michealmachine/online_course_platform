package com.double2and9.auth_service.integration;

import com.double2and9.auth_service.dto.request.CreatePermissionRequest;
import com.double2and9.auth_service.entity.Permission;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.repository.PermissionRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.base.enums.AuthErrorCode;
import com.double2and9.base.enums.PermissionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.Disabled;
import com.double2and9.auth_service.cache.PermissionCacheManager;

import java.util.HashSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PermissionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionCacheManager permissionCacheManager;

    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // 清理所有权限数据
        permissionRepository.deleteAll();
        roleRepository.deleteAll();  // 由于外键关系，先删除角色
        
        // 清除缓存
        permissionCacheManager.clearPermissionTree();

        // 创建测试权限数据
        testPermission = new Permission();
        testPermission.setName("test:read");
        testPermission.setResource("test");
        testPermission.setAction("read");
        testPermission.setDescription("Test Read Permission");
        testPermission.setType(PermissionType.API);
        testPermission.setRoles(new HashSet<>());  // 初始化roles集合
        testPermission = permissionRepository.save(testPermission);

        // 创建测试权限数据
        Permission userCreate = new Permission();
        userCreate.setName("user:create");
        userCreate.setResource("user");
        userCreate.setAction("create");
        userCreate.setDescription("创建用户");
        userCreate.setType(PermissionType.API);
        permissionRepository.save(userCreate);

        Permission roleCreate = new Permission();
        roleCreate.setName("role:create");
        roleCreate.setResource("role");
        roleCreate.setAction("create");
        roleCreate.setDescription("创建角色");
        roleCreate.setType(PermissionType.API);
        permissionRepository.save(roleCreate);

        Permission roleRead = new Permission();
        roleRead.setName("role:read");
        roleRead.setResource("role");
        roleRead.setAction("read");
        roleRead.setDescription("查看角色");
        roleRead.setType(PermissionType.API);
        permissionRepository.save(roleRead);

        // 验证数据是否正确保存
        var permissions = permissionRepository.findAll();
        assertThat(permissions).hasSize(4);  // 应该有4个权限
        assertThat(permissions.stream().map(Permission::getResource).distinct().collect(Collectors.toList()))
            .containsExactlyInAnyOrder("test", "user", "role");  // 验证资源是否都存在
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPermission_Success() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("user:delete");  // 使用一个新的、未使用的权限名
        request.setDescription("Delete User Permission");
        request.setResource("user");
        request.setAction("delete");
        request.setType(PermissionType.API);

        mockMvc.perform(post("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.resource").value(request.getResource()))
                .andExpect(jsonPath("$.action").value(request.getAction()))
                .andExpect(jsonPath("$.type").value(PermissionType.API.name()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createPermission_DuplicateName() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("test:read");  // 使用已存在的权限名
        request.setResource("test");
        request.setAction("read");
        request.setType(PermissionType.API);

        mockMvc.perform(post("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_ALREADY_EXISTS.getCode()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createPermission_NoPermission() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("user:create");
        request.setResource("user");
        request.setAction("create");

        mockMvc.perform(post("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_DENIED.getCode()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPermissions_Success() throws Exception {
        mockMvc.perform(get("/api/permissions")
                .param("pageNo", "1")
                .param("pageSize", "10")
                .param("resource", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].name").value("test:read"))
                .andExpect(jsonPath("$.counts").isNumber())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPermission_Success() throws Exception {
        mockMvc.perform(get("/api/permissions/{id}", testPermission.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test:read"))
                .andExpect(jsonPath("$.resource").value("test"))
                .andExpect(jsonPath("$.action").value("read"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePermission_Success() throws Exception {
        mockMvc.perform(delete("/api/permissions/{id}", testPermission.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePermission_InUse() throws Exception {
        // 创建角色并分配权限
        Role role = new Role();
        role.setName("ROLE_TEST");
        role.setDescription("Test Role");
        role.setPermissions(new HashSet<>());
        role.getPermissions().add(testPermission);
        roleRepository.save(role);

        // 确保权限和角色的关联被保存
        testPermission.getRoles().add(role);
        permissionRepository.save(testPermission);

        mockMvc.perform(delete("/api/permissions/{id}", testPermission.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_IN_USE.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.PERMISSION_IN_USE.getMessage()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getPermissionTree_Success() throws Exception {
        mockMvc.perform(get("/api/permissions/tree")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].resource", hasItems("test", "user", "role")));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getPermissionTree_Forbidden() throws Exception {
        mockMvc.perform(get("/api/permissions/tree"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPermissionTree_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/permissions/tree"))
                .andExpect(status().isUnauthorized());
    }
} 