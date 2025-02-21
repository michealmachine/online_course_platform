package com.double2and9.auth_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 角色分配权限请求DTO
 */
@Data
public class AssignPermissionRequest {
    /**
     * 要分配的权限ID列表
     */
    @NotEmpty(message = "权限ID列表不能为空")
    private List<Long> permissionIds;
} 