package com.double2and9.auth_service.dto.request;

import com.double2and9.base.enums.PermissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    @NotBlank(message = "权限名称不能为空")
    @Size(min = 2, max = 100, message = "权限名称长度必须在2-100之间")
    private String name;  // 例如: user:create

    @Size(max = 200, message = "描述长度不能超过200")
    private String description;

    @NotBlank(message = "资源类型不能为空")
    @Size(max = 100, message = "资源类型长度不能超过100")
    private String resource;  // 例如: user

    @NotBlank(message = "操作类型不能为空")
    @Size(max = 50, message = "操作类型长度不能超过50")
    private String action;    // 例如: create

    @NotNull(message = "权限类型不能为空")
    private PermissionType type = PermissionType.API;  // 默认为API类型

    @Size(max = 50, message = "授权范围长度不能超过50")
    private String scope;  // OAuth2授权范围，可选
} 