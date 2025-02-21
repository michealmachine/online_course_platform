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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    private Role testRole;
    private Permission testPermission;

    @BeforeEach
    void setUp() {
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignPermissions_Success() throws Exception {
        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(testPermission.getId()));

        mockMvc.perform(post("/api/roles/{roleId}/permissions", testRole.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void assignPermissions_RoleNotFound() throws Exception {
        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(testPermission.getId()));

        mockMvc.perform(post("/api/roles/{roleId}/permissions", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(AuthErrorCode.ROLE_NOT_EXISTS.getCode()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void assignPermissions_NoPermission() throws Exception {
        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(testPermission.getId()));

        mockMvc.perform(post("/api/roles/{roleId}/permissions", testRole.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getRolePermissions_Success() throws Exception {
        mockMvc.perform(get("/api/roles/{roleId}/permissions", testRole.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(testRole.getId()))
                .andExpect(jsonPath("$.roleName").value(testRole.getName()))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void revokePermission_Success() throws Exception {
        // 先分配权限
        testRole.getPermissions().add(testPermission);
        roleRepository.save(testRole);

        mockMvc.perform(delete("/api/roles/{roleId}/permissions/{permissionId}",
                testRole.getId(), testPermission.getId()))
                .andExpect(status().isNoContent());
    }
} 