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
import com.double2and9.auth_service.cache.PermissionCacheManager;

@Service
@RequiredArgsConstructor
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final UserRepository userRepository;
    private final PermissionCacheManager cacheManager;
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
        
        // 清除权限树缓存
        cacheManager.clearPermissionTree();
        
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
        
        // 清除相关缓存
        cacheManager.clearPermissionTree();
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
        System.out.println("=== getPermissionTree start ===");
        
        // 先尝试从缓存获取
        List<PermissionTreeNode> cachedTree = cacheManager.getPermissionTree();
        System.out.println("Cached tree: " + (cachedTree != null ? "found" : "not found"));
        
        if (cachedTree != null) {
            return cachedTree;
        }

        // 缓存未命中，从数据库获取并构建树
        List<PermissionTreeNode> tree = buildPermissionTree();
        System.out.println("Built tree size: " + tree.size());
        
        // 存入缓存
        cacheManager.cachePermissionTree(tree);
        
        return tree;
    }

    // 将原来的树构建逻辑抽取为私有方法
    private List<PermissionTreeNode> buildPermissionTree() {
        System.out.println("=== buildPermissionTree start ===");
        
        List<Permission> allPermissions = permissionRepository.findAll();
        System.out.println("All permissions size: " + allPermissions.size());
        System.out.println("All permissions: " + allPermissions.stream()
            .map(p -> p.getResource() + ":" + p.getAction())
            .collect(Collectors.joining(", ")));
        
        Map<String, List<Permission>> groupedPermissions = allPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getResource));
        System.out.println("Grouped resources: " + String.join(", ", groupedPermissions.keySet()));
        
        System.out.println("ResourceMetaMap contents:");
        resourceMetaMap.forEach((key, value) -> 
            System.out.println("  " + key + " -> description: " + value.getDescription() + 
                              ", sort: " + value.getSort()));
        
        return groupedPermissions.entrySet().stream()
                .map(entry -> {
                    String resource = entry.getKey();
                    System.out.println("Processing resource: " + resource);
                    
                    PermissionTreeNode node = new PermissionTreeNode();
                    node.setResource(resource);
                    
                    // 设置资源元数据
                    ResourceMeta meta = resourceMetaMap.getOrDefault(resource, 
                            new ResourceMeta(resource, Integer.MAX_VALUE));
                    System.out.println("Resource: " + resource + ", Meta: " + meta);
                    
                    node.setDescription(meta.getDescription());
                    node.setSort(meta.getSort());
                    
                    // 权限排序
                    node.setPermissions(entry.getValue().stream()
                            .sorted(Comparator.comparing(Permission::getAction))
                            .map(permissionMapper::toPermissionResponse)
                            .collect(Collectors.toList()));
                    System.out.println("Resource " + resource + " has " + node.getPermissions().size() + " permissions");
                    
                    return node;
                })
                .sorted(Comparator.comparing(PermissionTreeNode::getSort))  // 按配置的顺序排序
                .peek(node -> System.out.println("Sorted node: " + node.getResource() + 
                    "(" + node.getPermissions().size() + " permissions)"))
                .collect(Collectors.toList());
    }
} 