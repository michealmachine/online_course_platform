package com.double2and9.auth_service.dto.response;

import lombok.Data;
import java.util.List;

/**
 * 权限树节点
 */
@Data
public class PermissionTreeNode {
    /**
     * 资源类型
     */
    private String resource;
    
    /**
     * 资源描述
     */
    private String description;
    
    /**
     * 该资源下的权限列表
     */
    private List<PermissionResponse> permissions;
    
    /**
     * 资源排序码
     */
    private Integer sort;
} 