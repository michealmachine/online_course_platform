package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.PermissionResponse;
import com.double2and9.auth_service.dto.response.PermissionTreeNode;
import com.double2and9.auth_service.entity.Permission;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.mapper.PermissionMapper;
import com.double2and9.auth_service.repository.PermissionRepository;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.base.enums.PermissionType;
import com.double2and9.auth_service.dto.response.ResourceMeta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
public class PermissionServiceTest {

    @InjectMocks
    private PermissionService permissionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PermissionMapper permissionMapper;

    @Mock
    private Map<String, ResourceMeta> resourceMetaMap;

    @Test
    void getPermissionScopes_Success() {
        // 准备测试数据
        User user = new User();
        Role role = new Role();
        Permission permission = new Permission();
        permission.setType(PermissionType.OAUTH2);
        permission.setScope("read");
        role.setPermissions(Set.of(permission));
        user.setRoles(Set.of(role));
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // 执行测试
        Set<String> scopes = permissionService.getPermissionScopes(1L);
        
        // 验证结果
        assertThat(scopes).containsExactly("read");
    }

    @Test
    void hasPermissionScope_Success() {
        // 准备测试数据
        User user = new User();
        Role role = new Role();
        Permission permission = new Permission();
        permission.setType(PermissionType.OAUTH2);
        permission.setScope("read");
        role.setPermissions(Set.of(permission));
        user.setRoles(Set.of(role));
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // 执行测试
        boolean hasScope = permissionService.hasPermissionScope(1L, "read");
        
        // 验证结果
        assertThat(hasScope).isTrue();
    }

    @Test
    void getPermissionTree_Success() {
        // 准备测试数据
        Permission permission1 = new Permission();
        permission1.setResource("user");
        permission1.setAction("create");
        
        Permission permission2 = new Permission();
        permission2.setResource("user");
        permission2.setAction("read");
        
        Permission permission3 = new Permission();
        permission3.setResource("role");
        permission3.setAction("create");
        
        List<Permission> permissions = Arrays.asList(permission1, permission2, permission3);
        
        // 配置Mock行为
        when(permissionRepository.findAll()).thenReturn(permissions);
        when(permissionMapper.toPermissionResponse(any())).thenReturn(new PermissionResponse());
        when(resourceMetaMap.getOrDefault(eq("user"), any()))
                .thenReturn(new ResourceMeta("用户管理", 1));
        when(resourceMetaMap.getOrDefault(eq("role"), any()))
                .thenReturn(new ResourceMeta("角色管理", 2));
        
        // 执行测试
        List<PermissionTreeNode> result = permissionService.getPermissionTree();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());  // 应该有两个资源节点：user和role
        
        // 验证user节点
        PermissionTreeNode userNode = result.stream()
                .filter(node -> "user".equals(node.getResource()))
                .findFirst()
                .orElse(null);
        assertNotNull(userNode);
        assertEquals(2, userNode.getPermissions().size());
        
        // 验证role节点
        PermissionTreeNode roleNode = result.stream()
                .filter(node -> "role".equals(node.getResource()))
                .findFirst()
                .orElse(null);
        assertNotNull(roleNode);
        assertEquals(1, roleNode.getPermissions().size());
    }

    @Test
    void getPermissionTree_EmptyPermissions() {
        // 配置Mock行为 - 返回空列表
        when(permissionRepository.findAll()).thenReturn(Collections.emptyList());
        
        // 执行测试
        List<PermissionTreeNode> result = permissionService.getPermissionTree();
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getPermissionTree_SingleResource() {
        // 准备测试数据 - 只有一个资源类型
        Permission permission1 = new Permission();
        permission1.setResource("user");
        permission1.setAction("create");
        permission1.setName("创建用户");
        
        Permission permission2 = new Permission();
        permission2.setResource("user");
        permission2.setAction("delete");
        permission2.setName("删除用户");
        
        List<Permission> permissions = Arrays.asList(permission1, permission2);
        
        // 配置Mock行为
        when(permissionRepository.findAll()).thenReturn(permissions);
        PermissionResponse mockResponse = new PermissionResponse();
        when(permissionMapper.toPermissionResponse(any())).thenReturn(mockResponse);
        when(resourceMetaMap.getOrDefault(eq("user"), any()))
                .thenReturn(new ResourceMeta("用户管理", 1));
        
        // 执行测试
        List<PermissionTreeNode> result = permissionService.getPermissionTree();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user", result.get(0).getResource());
        assertEquals(2, result.get(0).getPermissions().size());
    }

    @Test
    void getPermissionTree_WithResourceMeta() {
        // 准备测试数据
        Permission userCreate = new Permission();
        userCreate.setResource("user");
        userCreate.setAction("create");
        
        Permission roleCreate = new Permission();
        roleCreate.setResource("role");
        roleCreate.setAction("create");
        
        List<Permission> permissions = Arrays.asList(roleCreate, userCreate);  // 故意打乱顺序
        
        // 配置Mock行为
        when(permissionRepository.findAll()).thenReturn(permissions);
        when(permissionMapper.toPermissionResponse(any())).thenReturn(new PermissionResponse());
        when(resourceMetaMap.getOrDefault(eq("user"), any()))
                .thenReturn(new ResourceMeta("用户管理", 1));
        when(resourceMetaMap.getOrDefault(eq("role"), any()))
                .thenReturn(new ResourceMeta("角色管理", 2));
        
        // 执行测试
        List<PermissionTreeNode> result = permissionService.getPermissionTree();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        
        // 验证排序
        assertEquals("user", result.get(0).getResource());
        assertEquals("用户管理", result.get(0).getDescription());
        assertEquals(1, result.get(0).getSort());
        
        assertEquals("role", result.get(1).getResource());
        assertEquals("角色管理", result.get(1).getDescription());
        assertEquals(2, result.get(1).getSort());
    }

    @Test
    void getPermissionTree_WithUnknownResource() {
        // 准备测试数据
        Permission unknownPermission = new Permission();
        unknownPermission.setResource("unknown");
        unknownPermission.setAction("test");
        
        // 配置Mock行为
        when(permissionRepository.findAll()).thenReturn(List.of(unknownPermission));
        when(permissionMapper.toPermissionResponse(any())).thenReturn(new PermissionResponse());
        when(resourceMetaMap.getOrDefault(eq("unknown"), any()))
                .thenReturn(new ResourceMeta("unknown", Integer.MAX_VALUE));
        
        // 执行测试
        List<PermissionTreeNode> result = permissionService.getPermissionTree();
        
        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("unknown", result.get(0).getResource());
        assertEquals("unknown", result.get(0).getDescription());
        assertEquals(Integer.MAX_VALUE, result.get(0).getSort());
    }
} 