package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.AssignPermissionRequest;
import com.double2and9.auth_service.dto.response.PermissionResponse;
import com.double2and9.auth_service.dto.response.RolePermissionResponse;
import com.double2and9.auth_service.entity.Permission;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.mapper.PermissionMapper;
import com.double2and9.auth_service.repository.PermissionRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.base.enums.AuthErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @InjectMocks
    private RoleService roleService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionMapper permissionMapper;

    @Test
    void assignPermissions_Success() {
        // 准备测试数据
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setPermissions(new HashSet<>());

        Permission permission = new Permission();
        permission.setId(1L);

        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(1L));

        // 配置Mock行为
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(request.getPermissionIds())).thenReturn(List.of(permission));

        // 执行测试
        assertDoesNotThrow(() -> roleService.assignPermissions(roleId, request));

        // 验证调用
        verify(roleRepository).save(role);
    }

    @Test
    void assignPermissions_RoleNotFound() {
        // 准备测试数据
        Long roleId = 1L;
        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(1L));

        // 配置Mock行为
        when(roleRepository.findById(roleId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class,
                () -> roleService.assignPermissions(roleId, request));
        assertEquals(AuthErrorCode.ROLE_NOT_EXISTS, exception.getErrorCode());
    }

    @Test
    void assignPermissions_PermissionNotFound() {
        // 准备测试数据
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setPermissions(new HashSet<>());

        AssignPermissionRequest request = new AssignPermissionRequest();
        request.setPermissionIds(List.of(1L));

        // 配置Mock行为
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findAllById(request.getPermissionIds())).thenReturn(Collections.emptyList());

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class,
                () -> roleService.assignPermissions(roleId, request));
        assertEquals(AuthErrorCode.PERMISSION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void revokePermission_Success() {
        // 准备测试数据
        Long roleId = 1L;
        Long permissionId = 1L;

        Permission permission = new Permission();
        permission.setId(permissionId);

        Role role = new Role();
        role.setId(roleId);
        role.setPermissions(new HashSet<>(Set.of(permission)));

        // 配置Mock行为
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));

        // 执行测试
        assertDoesNotThrow(() -> roleService.revokePermission(roleId, permissionId));

        // 验证调用
        verify(roleRepository).save(role);
        assertTrue(role.getPermissions().isEmpty());
    }

    @Test
    void getRolePermissions_Success() {
        // 准备测试数据
        Long roleId = 1L;
        Role role = new Role();
        role.setId(roleId);
        role.setName("ROLE_ADMIN");

        Permission permission = new Permission();
        permission.setId(1L);
        role.setPermissions(new HashSet<>(Set.of(permission)));

        // 配置Mock行为
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionMapper.toPermissionResponse(permission)).thenReturn(new PermissionResponse());

        // 执行测试
        RolePermissionResponse response = roleService.getRolePermissions(roleId);

        // 验证结果
        assertNotNull(response);
        assertEquals(roleId, response.getRoleId());
        assertEquals("ROLE_ADMIN", response.getRoleName());
        assertEquals(1, response.getPermissions().size());
    }

    // ... 其他测试方法 ...
} 