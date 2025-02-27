package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.entity.Role;
import com.double2and9.auth_service.entity.User;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.repository.UserRepository;
import com.double2and9.auth_service.security.AuthJwtTokenProvider;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtServiceTest {

    @Mock
    private AuthJwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtService jwtService;

    private static final String TEST_TOKEN = "test.token.string";
    private Claims accessTokenClaims;
    private Claims refreshTokenClaims;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 设置访问令牌的claims
        accessTokenClaims = Jwts.claims();
        accessTokenClaims.put("userId", "1");
        accessTokenClaims.put("clientId", "testClient");
        accessTokenClaims.put("scope", "read write");
        accessTokenClaims.put("type", "access_token");
        accessTokenClaims.setExpiration(new Date(System.currentTimeMillis() + 3600000));

        // 设置刷新令牌的claims
        refreshTokenClaims = Jwts.claims();
        refreshTokenClaims.put("userId", "1");
        refreshTokenClaims.put("clientId", "testClient");
        refreshTokenClaims.put("scope", "read write");
        refreshTokenClaims.put("type", "refresh_token");
        refreshTokenClaims.setExpiration(new Date(System.currentTimeMillis() + 3600000));

        // 设置默认的Mock行为
        when(jwtTokenProvider.generateToken(anyMap(), anyLong())).thenReturn(TEST_TOKEN);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setEmailVerified(true);
        testUser.setGivenName("John");
        testUser.setFamilyName("Doe");
        testUser.setUpdatedAt(LocalDateTime.now());

        // 设置默认的用户查找行为
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    }

    @Test
    void generateAccessToken_Success() {
        String userId = "1";
        String clientId = "testClient";
        String scope = "read write";

        when(jwtTokenProvider.generateToken(argThat(claims -> 
            "1".equals(claims.get("userId")) &&
            "testClient".equals(claims.get("clientId")) &&
            "read write".equals(claims.get("scope")) &&
            "access_token".equals(claims.get("type"))
        ), eq(3600L))).thenReturn(TEST_TOKEN);

        String token = jwtService.generateAccessToken(userId, clientId, scope);

        assertNotNull(token);
        assertEquals(TEST_TOKEN, token);
    }

    @Test
    void generateRefreshToken_Success() {
        String userId = "1";
        String clientId = "testClient";
        String scope = "read write";

        when(jwtTokenProvider.generateToken(argThat(claims -> 
            "1".equals(claims.get("userId")) &&
            "testClient".equals(claims.get("clientId")) &&
            "read write".equals(claims.get("scope")) &&
            "refresh_token".equals(claims.get("type"))
        ), eq(2592000L))).thenReturn(TEST_TOKEN);

        String refreshToken = jwtService.generateRefreshToken(userId, clientId, scope);

        assertNotNull(refreshToken);
        assertEquals(TEST_TOKEN, refreshToken);
    }

    @Test
    void validateAccessToken_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);
        Claims claims = jwtService.validateAccessToken(TEST_TOKEN);
        assertEquals(accessTokenClaims, claims);
    }

    @Test
    void validateAccessToken_InvalidType() {
        Claims invalidClaims = Jwts.claims();
        invalidClaims.put("userId", "testUser");
        invalidClaims.put("clientId", "testClient");
        invalidClaims.put("type", "refresh_token");
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(invalidClaims);

        assertThrows(IllegalArgumentException.class, () -> 
            jwtService.validateAccessToken(TEST_TOKEN));
    }

    @Test
    void validateRefreshToken_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(refreshTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);

        Claims result = jwtService.validateRefreshToken(TEST_TOKEN);

        assertNotNull(result);
        assertEquals("1", result.get("userId"));
        assertEquals("testClient", result.get("clientId"));
        assertEquals("refresh_token", result.get("type"));
    }

    @Test
    void validateRefreshToken_WrongTokenType() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class,
            () -> jwtService.validateRefreshToken(TEST_TOKEN));
        assertEquals(AuthErrorCode.TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void getUserIdFromToken_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);
        String userId = jwtService.getUserIdFromToken(TEST_TOKEN);
        assertEquals("1", userId);
    }

    @Test
    void getClientIdFromToken_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);
        String clientId = jwtService.getClientIdFromToken(TEST_TOKEN);
        assertEquals("testClient", clientId);
    }

    @Test
    void getScopeFromToken_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);
        String scope = jwtService.getScopeFromToken(TEST_TOKEN);
        assertEquals("read write", scope);
    }

    @Test
    void revokeToken_Success() {
        Claims claims = Jwts.claims();
        claims.setExpiration(new Date(System.currentTimeMillis() + 3600000));
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(claims);

        jwtService.revokeToken(TEST_TOKEN);

        verify(tokenBlacklistService).addToBlacklist(eq(TEST_TOKEN), anyLong());
    }

    @Test
    void revokeToken_InvalidToken() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN))
            .thenThrow(new RuntimeException(AuthErrorCode.TOKEN_INVALID.getMessage()));
        assertThrows(AuthException.class, () -> jwtService.revokeToken(TEST_TOKEN));
    }

    @Test
    void parseToken_RevokedToken() {
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(true);
        AuthException exception = assertThrows(AuthException.class, () -> 
            jwtService.parseToken(TEST_TOKEN));
        assertEquals(AuthErrorCode.TOKEN_REVOKED, exception.getErrorCode());
    }

    @Test
    void refreshTokens_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(refreshTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenProvider.generateToken(any(), anyLong())).thenReturn(TEST_TOKEN);

        TokenResponse response = jwtService.refreshTokens(TEST_TOKEN);

        assertNotNull(response);
        assertEquals(TEST_TOKEN, response.getAccessToken());
        assertEquals(TEST_TOKEN, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("read write", response.getScope());

        verify(jwtTokenProvider, times(2)).generateToken(any(), anyLong());
    }

    @Test
    void generateIdToken_Success() {
        // 准备测试数据
        String userId = "1";
        String clientId = "test-client";
        String nonce = "test-nonce";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateToken(any(), anyLong())).thenReturn("test-id-token");

        // 执行测试
        String idToken = jwtService.generateIdToken(userId, clientId, nonce);

        // 验证结果
        assertNotNull(idToken);
        assertEquals("test-id-token", idToken);
        verify(userRepository).findById(1L);
        verify(jwtTokenProvider).generateToken(any(), anyLong());
    }

    @Test
    void generateIdToken_WithOrganizationUser() {
        // 准备测试数据
        String userId = "1";
        String clientId = "test_client";
        
        User user = new User();
        user.setId(1L);
        user.setUsername("orguser");
        user.setEmail("test@example.com");
        user.setOrganizationId(123L);
        
        // 设置机构用户信息
        Role orgRole = new Role();
        orgRole.setName("ROLE_ORGANIZATION");
        user.setRoles(Set.of(orgRole));
        
        // 重置所有之前的mock
        reset(jwtTokenProvider);
        
        // 设置用户查找的mock
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // 设置token生成的mock，使用更宽松的参数匹配
        when(jwtTokenProvider.generateToken(any(Map.class), eq(3600L))).thenReturn("test.id.token");

        // 执行测试
        String idToken = jwtService.generateIdToken(userId, clientId, null);

        // 验证结果
        assertNotNull(idToken);
        assertEquals("test.id.token", idToken);
        
        // 验证调用
        verify(userRepository).findById(1L);
        verify(jwtTokenProvider).generateToken(argThat(claims -> {
            if (!(claims instanceof Map)) {
                return false;
            }
            Map<String, Object> claimsMap = (Map<String, Object>) claims;
            
            // 打印验证时的claims内容
            System.out.println("Verification claims: " + claimsMap);
            
            // 验证基本必需字段
            return "1".equals(claimsMap.get("sub")) &&
                   "test_client".equals(claimsMap.get("aud")) &&
                   "test@example.com".equals(claimsMap.get("email")) &&
                   "orguser".equals(claimsMap.get("name"));
        }), eq(3600L));
        
        // 确保没有其他调用
        verifyNoMoreInteractions(jwtTokenProvider);
    }

    @Test
    void generateIdToken_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () ->
            jwtService.generateIdToken("1", "test_client", null));

        assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(jwtTokenProvider, never()).generateToken(any(), anyLong());
    }

    @Test
    void generateAccessToken_InvalidUserId() {
        String userId = "invalid_id";
        String clientId = "testClient";
        String scope = "read write";

        AuthException exception = assertThrows(AuthException.class, () ->
            jwtService.generateAccessToken(userId, clientId, scope));

        assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void generateAccessToken_UserNotFound() {
        String userId = "999";
        String clientId = "testClient";
        String scope = "read write";

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () ->
            jwtService.generateAccessToken(userId, clientId, scope));

        assertEquals(AuthErrorCode.TOKEN_GENERATE_ERROR, exception.getErrorCode());
        verify(userRepository).findById(999L);
    }

    @Test
    void generateAccessToken_WithOrganizationId() {
        String userId = "1";
        String clientId = "testClient";
        String scope = "read write";

        // 创建带有机构ID的用户
        User orgUser = new User();
        orgUser.setId(1L);
        orgUser.setOrganizationId(123L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(orgUser));

        when(jwtTokenProvider.generateToken(argThat(claims -> 
            "1".equals(claims.get("userId")) &&
            "testClient".equals(claims.get("clientId")) &&
            "read write".equals(claims.get("scope")) &&
            "access_token".equals(claims.get("type")) &&
            123L == (Long)claims.get("organization_id")
        ), eq(3600L))).thenReturn(TEST_TOKEN);

        String token = jwtService.generateAccessToken(userId, clientId, scope);

        assertNotNull(token);
        assertEquals(TEST_TOKEN, token);
    }

    @Test
    void generateRefreshToken_InvalidUserId() {
        String userId = "invalid_id";
        String clientId = "testClient";
        String scope = "read write";

        AuthException exception = assertThrows(AuthException.class, () ->
            jwtService.generateRefreshToken(userId, clientId, scope));

        assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void generateRefreshToken_UserNotFound() {
        String userId = "999";
        String clientId = "testClient";
        String scope = "read write";

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class, () ->
            jwtService.generateRefreshToken(userId, clientId, scope));

        assertEquals(AuthErrorCode.TOKEN_GENERATE_ERROR, exception.getErrorCode());
        verify(userRepository).findById(999L);
    }

    @Test
    void generateRefreshToken_WithOrganizationId() {
        String userId = "1";
        String clientId = "testClient";
        String scope = "read write";

        // 创建带有机构ID的用户
        User orgUser = new User();
        orgUser.setId(1L);
        orgUser.setOrganizationId(123L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(orgUser));

        when(jwtTokenProvider.generateToken(argThat(claims -> 
            "1".equals(claims.get("userId")) &&
            "testClient".equals(claims.get("clientId")) &&
            "read write".equals(claims.get("scope")) &&
            "refresh_token".equals(claims.get("type")) &&
            123L == (Long)claims.get("organization_id")
        ), eq(2592000L))).thenReturn(TEST_TOKEN);

        String token = jwtService.generateRefreshToken(userId, clientId, scope);

        assertNotNull(token);
        assertEquals(TEST_TOKEN, token);
    }

    @Test
    void introspectIdToken_Success() {
        // 准备测试数据
        Claims idTokenClaims = Jwts.claims();
        idTokenClaims.put("sub", "1");  // 用户ID
        idTokenClaims.put("aud", "test_client");  // 客户端ID
        idTokenClaims.put("email", "test@example.com");
        idTokenClaims.put("name", "test_user");
        idTokenClaims.setExpiration(new Date(System.currentTimeMillis() + 3600000));
        idTokenClaims.setIssuedAt(new Date());

        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(idTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);

        // 执行测试
        TokenIntrospectionResponse response = jwtService.introspectIdToken(TEST_TOKEN);

        // 验证结果
        assertNotNull(response);
        assertTrue(response.isActive());
        assertEquals("1", response.getUserId());
        assertEquals("test_client", response.getClientId());
        assertEquals("openid profile email", response.getScope());
        assertEquals("id_token", response.getTokenType());
        assertNotNull(response.getExp());
        assertNotNull(response.getIat());
    }

    @Test
    void introspectIdToken_InvalidToken() {
        // 模拟令牌验证失败
        when(jwtTokenProvider.validateToken(TEST_TOKEN))
            .thenThrow(new RuntimeException("Invalid token"));

        // 执行测试
        TokenIntrospectionResponse response = jwtService.introspectIdToken(TEST_TOKEN);

        // 验证结果
        assertNotNull(response);
        assertFalse(response.isActive());
        assertNull(response.getUserId());
        assertNull(response.getClientId());
        assertNull(response.getScope());
        assertNull(response.getTokenType());
    }

    @Test
    void introspectIdToken_ExpiredToken() {
        // 准备过期的令牌声明
        Claims expiredClaims = Jwts.claims();
        expiredClaims.setExpiration(new Date(System.currentTimeMillis() - 1000));
        
        when(jwtTokenProvider.validateToken(TEST_TOKEN))
            .thenThrow(new ExpiredJwtException(null, expiredClaims, "Token expired"));

        // 执行测试
        TokenIntrospectionResponse response = jwtService.introspectIdToken(TEST_TOKEN);

        // 验证结果
        assertNotNull(response);
        assertFalse(response.isActive());
    }

    @Test
    void introspectIdToken_BlacklistedToken() {
        // 准备测试数据
        Claims idTokenClaims = Jwts.claims();
        idTokenClaims.put("sub", "1");
        idTokenClaims.put("aud", "test_client");
        
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(idTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(true);

        // 执行测试
        TokenIntrospectionResponse response = jwtService.introspectIdToken(TEST_TOKEN);

        // 验证结果
        assertNotNull(response);
        assertFalse(response.isActive());
    }
} 