package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.AssignPermissionRequest;
import com.double2and9.auth_service.dto.response.RolePermissionResponse;
import com.double2and9.auth_service.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "角色管理", description = "角色权限相关接口")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @Operation(summary = "为角色分配权限")
    @PostMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignPermissions(
            @PathVariable Long roleId,
            @RequestBody @Valid AssignPermissionRequest request) {
        roleService.assignPermissions(roleId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "回收角色的某个权限")
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> revokePermission(
            @PathVariable Long roleId,
            @PathVariable Long permissionId) {
        roleService.revokePermission(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "获取角色的权限列表")
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RolePermissionResponse> getRolePermissions(
            @PathVariable Long roleId) {
        return ResponseEntity.ok(roleService.getRolePermissions(roleId));
    }
} 