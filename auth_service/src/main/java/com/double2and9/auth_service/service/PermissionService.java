package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.request.CreatePermissionRequest;
import com.double2and9.auth_service.dto.response.PermissionResponse;
import com.double2and9.auth_service.dto.response.PermissionTreeNode;
import com.double2and9.auth_service.entity.Permission;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.mapper.PermissionMapper;
import com.double2and9.auth_service.repository.PermissionRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.base.enums.AuthErrorCode;
import com.double2and9.base.enums.PermissionType;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import com.double2and9.auth_service.dto.response.ResourceMeta;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final UserRepository userRepository;
    private final Map<String, ResourceMeta> resourceMetaMap;

    /**
     * 创建新的权限
     * 
     * @param request 权限创建请求
     * @return 创建成功的权限信息
     * @throws AuthException 当权限名已存在时抛出异常
     */
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new AuthException(AuthErrorCode.PERMISSION_ALREADY_EXISTS, HttpStatus.BAD_REQUEST);
        }

        Permission permission = permissionMapper.toEntity(request);
        permission = permissionRepository.save(permission);
        return permissionMapper.toPermissionResponse(permission);
    }

    /**
     * 分页查询权限列表
     * 
     * @param resource 资源类型筛选条件
     * @param action 操作类型筛选条件
     * @param pageParams 分页参数
     * @return 分页的权限列表
     */
    public PageResult<PermissionResponse> getPermissions(String resource, String action, PageParams pageParams) {
        // 构建分页查询条件
        Page<Permission> permissionPage = permissionRepository.findByResourceContainingAndActionContaining(
                resource != null ? resource : "",
                action != null ? action : "",
                PageRequest.of(pageParams.getPageNo().intValue() - 1, pageParams.getPageSize().intValue())
        );

        // 转换为PageResult
        return new PageResult<>(
                permissionPage.getContent().stream()
                        .map(permissionMapper::toPermissionResponse)
                        .collect(Collectors.toList()),
                permissionPage.getTotalElements(),
                pageParams.getPageNo(),
                pageParams.getPageSize()
        );
    }

    public PermissionResponse getPermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.PERMISSION_NOT_FOUND));
        return permissionMapper.toPermissionResponse(permission);
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AuthException(AuthErrorCode.PERMISSION_NOT_FOUND));
        
        // 检查权限是否被使用
        if (!permission.getRoles().isEmpty()) {
            throw new AuthException(AuthErrorCode.PERMISSION_IN_USE, HttpStatus.BAD_REQUEST);  // 指定400状态码
        }
        
        permissionRepository.delete(permission);
    }

    /**
     * 获取用户的OAuth2授权范围
     *
     * @param userId 用户ID
     * @return 用户拥有的OAuth2授权范围集合
     * @throws AuthException 当用户不存在时
     */
    public Set<String> getPermissionScopes(Long userId) {
        return userRepository.findById(userId)
            .map(user -> user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .filter(permission -> PermissionType.OAUTH2.equals(permission.getType()))
                .map(Permission::getScope)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
            .orElseThrow(() -> new AuthException(AuthErrorCode.USER_NOT_FOUND));
    }

    /**
     * 检查用户是否拥有特定的OAuth2授权范围
     *
     * @param userId 用户ID
     * @param scope 要检查的授权范围
     * @return 是否拥有该授权范围
     */
    public boolean hasPermissionScope(Long userId, String scope) {
        return getPermissionScopes(userId).contains(scope);
    }

    /**
     * 获取权限树
     * 按资源类型分组权限
     *
     * @return 权限树列表
     */
    public List<PermissionTreeNode> getPermissionTree() {
        List<Permission> allPermissions = permissionRepository.findAll();
        Map<String, List<Permission>> groupedPermissions = allPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getResource));
        
        return groupedPermissions.entrySet().stream()
                .map(entry -> {
                    PermissionTreeNode node = new PermissionTreeNode();
                    String resource = entry.getKey();
                    node.setResource(resource);
                    
                    // 设置资源元数据
                    ResourceMeta meta = resourceMetaMap.getOrDefault(resource, 
                            new ResourceMeta(resource, Integer.MAX_VALUE));
                    node.setDescription(meta.getDescription());
                    node.setSort(meta.getSort());
                    
                    // 权限排序
                    node.setPermissions(entry.getValue().stream()
                            .sorted(Comparator.comparing(Permission::getAction))
                            .map(permissionMapper::toPermissionResponse)
                            .collect(Collectors.toList()));
                    return node;
                })
                .sorted(Comparator.comparing(PermissionTreeNode::getSort))  // 按配置的顺序排序
                .collect(Collectors.toList());
    }
} 