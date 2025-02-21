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

import java.util.HashSet;

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

    private Permission testPermission;

    @BeforeEach
    void setUp() {
        // 创建测试权限
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
        permissionRepository.save(userCreate);

        Permission userRead = new Permission();
        userRead.setName("user:read");
        userRead.setResource("user");
        userRead.setAction("read");
        userRead.setDescription("查看用户");
        permissionRepository.save(userRead);

        Permission roleCreate = new Permission();
        roleCreate.setName("role:create");
        roleCreate.setResource("role");
        roleCreate.setAction("create");
        roleCreate.setDescription("创建角色");
        permissionRepository.save(roleCreate);
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
                .andExpect(jsonPath("$[*].resource", hasItems("user", "role")))
                .andExpect(jsonPath("$[?(@.resource=='user')].description").value(hasItem("用户管理")))
                .andExpect(jsonPath("$[?(@.resource=='user')].sort").value(hasItem(1)))
                .andExpect(jsonPath("$[?(@.resource=='role')].description").value(hasItem("角色管理")))
                .andExpect(jsonPath("$[?(@.resource=='role')].sort").value(hasItem(2)));
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