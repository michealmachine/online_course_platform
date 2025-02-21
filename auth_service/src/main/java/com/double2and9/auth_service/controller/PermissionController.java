package com.double2and9.auth_service.controller;

import com.double2and9.auth_service.dto.request.CreatePermissionRequest;
import com.double2and9.auth_service.dto.response.PermissionResponse;
import com.double2and9.auth_service.dto.response.PermissionTreeNode;
import com.double2and9.auth_service.entity.Permission;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.mapper.PermissionMapper;
import com.double2and9.auth_service.service.PermissionService;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "权限管理接口", description = "权限的增删改查")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {
    
    private final PermissionService permissionService;
    private final PermissionMapper permissionMapper;

    /**
     * 创建新的权限
     * 
     * @param request 权限创建请求，包含权限名称、描述、资源类型、操作类型
     * @return 创建成功的权限信息
     */
    @Operation(summary = "创建权限")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        PermissionResponse response = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 分页查询权限列表，支持按资源类型和操作类型筛选
     * 
     * @param resource 资源类型，可选
     * @param action 操作类型，可选
     * @param pageNo 页码
     * @param pageSize 每页大小
     * @return 分页的权限列表
     */
    @Operation(summary = "获取权限列表")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResult<PermissionResponse>> getPermissions(
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") Long pageNo,
            @RequestParam(defaultValue = "10") Long pageSize) {
        PageParams pageParams = new PageParams(pageNo, pageSize);
        return ResponseEntity.ok(permissionService.getPermissions(resource, action, pageParams));
    }

    /**
     * 获取指定权限的详细信息
     * 
     * @param id 权限ID
     * @return 权限详细信息
     */
    @Operation(summary = "获取权限详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PermissionResponse> getPermission(@PathVariable Long id) {
        return ResponseEntity.ok(permissionService.getPermission(id));
    }

    /**
     * 删除指定的权限
     * 注意：只有未被任何角色使用的权限才能被删除
     * 
     * @param id 权限ID
     * @return 删除成功返回204状态码
     */
    @Operation(summary = "删除权限")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "获取权限树")
    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PermissionTreeNode>> getPermissionTree() {
        return ResponseEntity.ok(permissionService.getPermissionTree());
    }
} 