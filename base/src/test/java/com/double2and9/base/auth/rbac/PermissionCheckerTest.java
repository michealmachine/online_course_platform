package com.double2and9.base.auth.rbac;

import com.double2and9.base.enums.PermissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

/**
 * PermissionChecker工具类的单元测试
 */
class PermissionCheckerTest {

    private AuthUser mockUser;
    private AuthRole mockRole;
    private AuthPermission mockPermission;
    private AuthPermission mockOAuth2Permission;

    @BeforeEach
    void setUp() {
        // 创建模拟对象
        mockUser = mock(AuthUser.class);
        mockRole = mock(AuthRole.class);
        mockPermission = mock(AuthPermission.class);
        mockOAuth2Permission = mock(AuthPermission.class);
        
        // 配置模拟行为
        when(mockUser.isEnabled()).thenReturn(true);
        
        // 使用doReturn...when语法，而不是when...thenReturn
        doReturn(Collections.singleton(mockRole)).when(mockUser).getRoles();
        
        when(mockRole.getName()).thenReturn("ROLE_ADMIN");
        
        // 同样使用doReturn...when语法处理权限集合
        Set<AuthPermission> permissions = new HashSet<>();
        permissions.add(mockPermission);
        permissions.add(mockOAuth2Permission);
        doReturn(permissions).when(mockRole).getPermissions();
        
        when(mockPermission.getResource()).thenReturn("user");
        when(mockPermission.getAction()).thenReturn("read");
        when(mockPermission.getType()).thenReturn(PermissionType.API);
        when(mockPermission.getScope()).thenReturn(null);  // API类型没有scope
        
        when(mockOAuth2Permission.getResource()).thenReturn("profile");
        when(mockOAuth2Permission.getAction()).thenReturn("read");
        when(mockOAuth2Permission.getType()).thenReturn(PermissionType.OAUTH2);
        when(mockOAuth2Permission.getScope()).thenReturn("read_profile");
    }

    @Test
    void shouldReturnTrueWhenUserHasPermission() {
        assertTrue(PermissionChecker.hasPermission(mockUser, "user", "read"));
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotHavePermission() {
        assertFalse(PermissionChecker.hasPermission(mockUser, "user", "write"));
    }

    @Test
    void shouldReturnFalseForNullUser() {
        assertFalse(PermissionChecker.hasPermission(null, "user", "read"));
    }

    @Test
    void shouldReturnFalseForDisabledUser() {
        when(mockUser.isEnabled()).thenReturn(false);
        assertFalse(PermissionChecker.hasPermission(mockUser, "user", "read"));
    }

    @Test
    void shouldReturnTrueWhenUserHasRole() {
        assertTrue(PermissionChecker.hasRole(mockUser, "ROLE_ADMIN"));
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotHaveRole() {
        assertFalse(PermissionChecker.hasRole(mockUser, "ROLE_USER"));
    }

    @Test
    void shouldGetPermissionScopes() {
        Set<String> scopes = PermissionChecker.getPermissionScopes(mockUser);
        assertEquals(1, scopes.size());
        assertTrue(scopes.contains("read_profile"));
    }

    @Test
    void shouldReturnEmptyScopesForNullUser() {
        Set<String> scopes = PermissionChecker.getPermissionScopes(null);
        assertTrue(scopes.isEmpty());
    }

    @Test
    void shouldReturnTrueWhenUserHasPermissionScope() {
        assertTrue(PermissionChecker.hasPermissionScope(mockUser, "read_profile"));
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotHavePermissionScope() {
        assertFalse(PermissionChecker.hasPermissionScope(mockUser, "write_profile"));
    }
} 