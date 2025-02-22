package com.double2and9.auth_service.service;

import com.double2and9.auth_service.cache.PermissionCacheManager;
import com.double2and9.auth_service.dto.request.AssignPermissionRequest;
import com.double2and9.auth_service.dto.response.RolePermissionResponse;
import com.double2and9.auth_service.entity.Permission;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.mapper.PermissionMapper;
import com.double2and9.auth_service.repository.PermissionRepository;
import com.double2and9.auth_service.repository.RoleRepository;
import com.double2and9.base.enums.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final PermissionCacheManager cacheManager;

    /**
     * 为角色分配权限
     *
     * @param roleId 角色ID
     * @param request 权限分配请求
     * @throws AuthException 当角色不存在或权限不存在时
     */
    @Transactional
    public void assignPermissions(Long roleId, AssignPermissionRequest request) {
        // 获取角色
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.ROLE_NOT_EXISTS, HttpStatus.NOT_FOUND));

        // 获取要分配的权限列表
        List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
        if (permissions.size() != request.getPermissionIds().size()) {
            throw new AuthException(AuthErrorCode.PERMISSION_NOT_FOUND, HttpStatus.BAD_REQUEST);
        }

        // 更新角色的权限
        Set<Permission> permissionSet = new HashSet<>(permissions);
        role.setPermissions(permissionSet);
        roleRepository.save(role);
        
        // 清除相关缓存
        cacheManager.clearRolePermissions(roleId);
        cacheManager.clearPermissionTree();
    }

    /**
     * 回收角色的某个权限
     *
     * @param roleId 角色ID
     * @param permissionId 权限ID
     * @throws AuthException 当角色不存在或权限不存在时
     */
    @Transactional
    public void revokePermission(Long roleId, Long permissionId) {
        // 获取角色
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.ROLE_NOT_EXISTS, HttpStatus.NOT_FOUND));

        // 获取要回收的权限
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.PERMISSION_NOT_FOUND, HttpStatus.NOT_FOUND));

        // 移除权限
        Set<Permission> permissions = new HashSet<>(role.getPermissions());
        if (!permissions.remove(permission)) {
            throw new AuthException(AuthErrorCode.PERMISSION_NOT_FOUND, HttpStatus.BAD_REQUEST);
        }
        role.setPermissions(permissions);
        roleRepository.save(role);
        
        // 清除相关缓存
        cacheManager.clearRolePermissions(roleId);
        cacheManager.clearPermissionTree();
    }

    /**
     * 获取角色的权限列表
     *
     * @param roleId 角色ID
     * @return 角色权限信息
     * @throws AuthException 当角色不存在时
     */
    public RolePermissionResponse getRolePermissions(Long roleId) {
        // 获取角色
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.ROLE_NOT_EXISTS, HttpStatus.NOT_FOUND));

        // 构建响应
        RolePermissionResponse response = new RolePermissionResponse();
        response.setRoleId(role.getId());
        response.setRoleName(role.getName());
        response.setPermissions(role.getPermissions().stream()
                .map(permissionMapper::toPermissionResponse)
                .collect(Collectors.toList()));

        return response;
    }
} 