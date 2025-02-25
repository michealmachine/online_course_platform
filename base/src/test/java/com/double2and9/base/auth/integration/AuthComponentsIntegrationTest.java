package com.double2and9.base.auth.integration;

import com.double2and9.base.auth.jwt.DefaultTokenVerifier;
import com.double2and9.base.auth.jwt.JwtProperties;
import com.double2and9.base.auth.rbac.AuthPermission;
import com.double2and9.base.auth.rbac.AuthRole;
import com.double2and9.base.auth.rbac.AuthUser;
import com.double2and9.base.auth.rbac.PermissionChecker;
import com.double2and9.base.auth.token.RedisTokenBlacklistService;
import com.double2and9.base.auth.token.TokenBlacklistService;
import com.double2and9.base.auth.util.JwtUtils;
import com.double2and9.base.enums.PermissionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 测试JWT验证和权限检查组件的集成
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 设置宽松模式以避免UnnecessaryStubbingException
class AuthComponentsIntegrationTest {

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private RedisTemplate<String, String> redisTemplate;
    
    @Mock
    private ValueOperations<String, String> valueOperations;

    private DefaultTokenVerifier tokenVerifier;
    private TokenBlacklistService blacklistService;
    private AuthUser testUser;

    private final String TEST_SECRET = "ZDY1NmE3ZjI0ODdiNGZkZWJmZjk5MjExYjU4MzRlMzE2NzIwZWM0MjJmNmFlOWY5NzM2ZmRkMGM5NTYzYzE3YQ==";
    private final String TEST_USERNAME = "testuser";

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
        // 预先设置redisTemplate.opsForValue()以避免NullPointerException
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        tokenVerifier = new DefaultTokenVerifier(jwtProperties);
        blacklistService = new RedisTokenBlacklistService(redisTemplate);
        
        // 创建测试用户和权限
        testUser = createTestUser();
    }

    @Test
    void shouldValidateTokenAndCheckPermission() {
        // 1. 生成令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", TEST_USERNAME);
        claims.put("userId", "1");
        claims.put("type", "access_token");
        String token = JwtUtils.generateToken(TEST_SECRET, claims, 3600);
        
        // 2. 验证令牌
        boolean isValid = tokenVerifier.isTokenValid(token);
        assertTrue(isValid);
        
        // 3. 从令牌获取用户名
        String username = tokenVerifier.getUsernameFromToken(token);
        assertEquals(TEST_USERNAME, username);
        
        // 4. 检查用户权限
        boolean hasPermission = PermissionChecker.hasPermission(testUser, "user", "read");
        assertTrue(hasPermission);
        
        // 5. 检查用户没有的权限
        boolean hasNoPermission = PermissionChecker.hasPermission(testUser, "user", "delete");
        assertFalse(hasNoPermission);
    }

    @Test
    void shouldRevokeToken() {
        // 1. 生成令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", TEST_USERNAME);
        String token = JwtUtils.generateToken(TEST_SECRET, claims, 3600);
        
        // 2. 模拟黑名单服务行为
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        
        // 3. 检查令牌是否在黑名单中
        boolean isBlacklisted = blacklistService.isBlacklisted(token);
        assertFalse(isBlacklisted);
        
        // 4. 添加令牌到黑名单
        blacklistService.addToBlacklist(token, 3600);
        verify(valueOperations, times(1)).set(anyString(), anyString(), anyLong(), any());
        
        // 5. 模拟令牌现在在黑名单中
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        
        // 6. 再次检查令牌是否在黑名单中
        isBlacklisted = blacklistService.isBlacklisted(token);
        assertTrue(isBlacklisted);
    }

    /**
     * 创建一个具有角色和权限的测试用户
     */
    private AuthUser createTestUser() {
        // 创建模拟权限
        AuthPermission readPermission = mock(AuthPermission.class);
        when(readPermission.getResource()).thenReturn("user");
        when(readPermission.getAction()).thenReturn("read");
        when(readPermission.getType()).thenReturn(PermissionType.API);
        
        AuthPermission writePermission = mock(AuthPermission.class);
        when(writePermission.getResource()).thenReturn("user");
        when(writePermission.getAction()).thenReturn("write");
        when(writePermission.getType()).thenReturn(PermissionType.API);
        
        AuthPermission oauthPermission = mock(AuthPermission.class);
        when(oauthPermission.getResource()).thenReturn("profile");
        when(oauthPermission.getAction()).thenReturn("read");
        when(oauthPermission.getType()).thenReturn(PermissionType.OAUTH2);
        when(oauthPermission.getScope()).thenReturn("read_profile");
        
        // 创建模拟角色
        AuthRole role = mock(AuthRole.class);
        when(role.getName()).thenReturn("ROLE_USER");
        Set<AuthPermission> permissions = new HashSet<>(Arrays.asList(readPermission, writePermission, oauthPermission));
        doReturn(permissions).when(role).getPermissions();
        
        // 创建模拟用户
        AuthUser user = mock(AuthUser.class);
        when(user.getUsername()).thenReturn(TEST_USERNAME);
        when(user.isEnabled()).thenReturn(true);
        doReturn(Collections.singleton(role)).when(user).getRoles();
        
        return user;
    }
} 