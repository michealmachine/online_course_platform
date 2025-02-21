package com.double2and9.auth_service.mapper;

import com.double2and9.auth_service.dto.request.CreatePermissionRequest;
import com.double2and9.auth_service.dto.response.PermissionResponse;
import com.double2and9.auth_service.entity.Permission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 权限实体与DTO之间的转换器
 */
@Mapper(componentModel = "spring")
public interface PermissionMapper {
    
    /**
     * 将Permission实体转换为PermissionResponse DTO
     *
     * @param permission 权限实体
     * @return 权限响应DTO
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "resource", source = "resource")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "scope", source = "scope")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    PermissionResponse toPermissionResponse(Permission permission);

    /**
     * 将CreatePermissionRequest转换为Permission实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "type", source = "type")
    @Mapping(target = "scope", source = "scope")
    Permission toEntity(CreatePermissionRequest request);
} 