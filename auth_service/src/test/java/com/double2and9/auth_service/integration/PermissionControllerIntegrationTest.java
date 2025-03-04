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
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import com.double2and9.auth_service.cache.PermissionCacheManager;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class PermissionControllerIntegrationTest extends BaseOAuth2IntegrationTest {

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private PermissionCacheManager permissionCacheManager;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Permission testPermission;

    @BeforeEach
    public void setUp() throws Exception {
        // 先清理用户角色关联
        jdbcTemplate.execute("DELETE FROM user_roles");
        
        // 清理角色权限关联
        jdbcTemplate.execute("DELETE FROM role_permissions");
        
        // 清理所有权限数据
        permissionRepository.deleteAll();
        
        // 清理角色数据
        roleRepository.deleteAll();  // 由于外键关系，先删除角色
        
        // 清除缓存
        permissionCacheManager.clearPermissionTree();

        // 调用父类的 setUp 来创建必要的角色和用户
        super.setUp();

        // 创建测试权限数据
        testPermission = new Permission();
        testPermission.setName("test:read");
        testPermission.setResource("test");
        testPermission.setAction("read");
        testPermission.setDescription("Test Read Permission");
        testPermission.setType(PermissionType.API);
        testPermission.setScope("test.read");  // 添加scope
        testPermission.setRoles(new HashSet<>());
        testPermission = permissionRepository.save(testPermission);

        // 创建测试权限数据
        Permission userCreate = new Permission();
        userCreate.setName("user:create");
        userCreate.setResource("user");
        userCreate.setAction("create");
        userCreate.setDescription("创建用户");
        userCreate.setType(PermissionType.API);
        userCreate.setScope("user.create");  // 添加scope
        userCreate.setRoles(new HashSet<>());
        permissionRepository.save(userCreate);

        Permission roleCreate = new Permission();
        roleCreate.setName("role:create");
        roleCreate.setResource("role");
        roleCreate.setAction("create");
        roleCreate.setDescription("创建角色");
        roleCreate.setType(PermissionType.API);
        roleCreate.setScope("role.create");  // 添加scope
        roleCreate.setRoles(new HashSet<>());
        permissionRepository.save(roleCreate);

        Permission roleRead = new Permission();
        roleRead.setName("role:read");
        roleRead.setResource("role");
        roleRead.setAction("read");
        roleRead.setDescription("查看角色");
        roleRead.setType(PermissionType.API);
        roleRead.setScope("role.read");  // 添加scope
        roleRead.setRoles(new HashSet<>());
        permissionRepository.save(roleRead);

        // 确保数据已经提交到数据库
        permissionRepository.flush();
        
        // 验证数据是否正确保存
        var permissions = permissionRepository.findAll();
        assertThat(permissions).hasSize(4);  // 应该有4个权限
        assertThat(permissions.stream().map(Permission::getResource).distinct().collect(Collectors.toList()))
            .containsExactlyInAnyOrder("test", "user", "role");  // 验证资源是否都存在
    }

    private void setupRoles() {
        // 创建必要的角色
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        adminRole.setDescription("管理员角色");
        adminRole = roleRepository.save(adminRole);

        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        userRole.setDescription("普通用户角色");
        roleRepository.save(userRole);
    }

    @Test
    void createPermission_Success() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("user:delete");  // 使用一个新的、未使用的权限名
        request.setDescription("Delete User Permission");
        request.setResource("user");
        request.setAction("delete");
        request.setType(PermissionType.API);
        request.setScope("user.delete");  // 添加scope字段

        mockMvc.perform(post("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(request.getName()))
                .andExpect(jsonPath("$.description").value(request.getDescription()))
                .andExpect(jsonPath("$.resource").value(request.getResource()))
                .andExpect(jsonPath("$.action").value(request.getAction()))
                .andExpect(jsonPath("$.type").value(request.getType().name()))
                .andExpect(jsonPath("$.scope").value(request.getScope()));
    }

    @Test
    void createPermission_DuplicateName() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("test:read");  // 使用已存在的权限名
        request.setResource("test");
        request.setAction("read");
        request.setType(PermissionType.API);
        request.setScope("test.read");
        request.setDescription("Test Permission");

        mockMvc.perform(post("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_ALREADY_EXISTS.getCode()));
    }

    @Test
    void createPermission_NoPermission() throws Exception {
        // 使用普通用户token
        setupUserWithToken();

        CreatePermissionRequest request = new CreatePermissionRequest();
        request.setName("user:create");
        request.setResource("user");
        request.setAction("create");
        request.setType(PermissionType.API);
        request.setScope("user.create");
        request.setDescription("Create User Permission");

        mockMvc.perform(post("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_DENIED.getCode()));
    }

    @Test
    void getPermissions_Success() throws Exception {
        mockMvc.perform(get("/api/permissions")
                .header("Authorization", "Bearer " + accessToken)
                .param("pageNo", "1")
                .param("pageSize", "10")
                .param("resource", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].name").value("test:read"))
                .andExpect(jsonPath("$.items[0].resource").value("test"))
                .andExpect(jsonPath("$.items[0].action").value("read"))
                .andExpect(jsonPath("$.items[0].type").value("API"))
                .andExpect(jsonPath("$.items[0].scope").value("test.read"))
                .andExpect(jsonPath("$.counts").isNumber())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    void getPermission_Success() throws Exception {
        mockMvc.perform(get("/api/permissions/{id}", testPermission.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test:read"))
                .andExpect(jsonPath("$.resource").value("test"))
                .andExpect(jsonPath("$.action").value("read"))
                .andExpect(jsonPath("$.type").value("API"))
                .andExpect(jsonPath("$.scope").value("test.read"));
    }

    @Test
    void deletePermission_Success() throws Exception {
        mockMvc.perform(delete("/api/permissions/{id}", testPermission.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());
    }

    @Test
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

        mockMvc.perform(delete("/api/permissions/{id}", testPermission.getId())
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.PERMISSION_IN_USE.getCode()))
                .andExpect(jsonPath("$.message").value(AuthErrorCode.PERMISSION_IN_USE.getMessage()));
    }

    @Test
    void getPermissionTree_Success() throws Exception {
        mockMvc.perform(get("/api/permissions/tree")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].resource", hasItems("test", "user", "role")));
    }

    @Test
    void getPermissionTree_Forbidden() throws Exception {
        // 使用普通用户token
        setupUserWithToken();

        mockMvc.perform(get("/api/permissions/tree")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPermissionTree_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/permissions/tree"))
                .andExpect(status().isUnauthorized());
    }
} 