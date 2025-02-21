package com.double2and9.auth_service.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 角色权限响应DTO
 */
@Data
public class RolePermissionResponse {
    /**
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 角色名称
     */
    private String roleName;
    
    /**
     * 角色拥有的权限列表
     */
    private List<PermissionResponse> permissions;
} 