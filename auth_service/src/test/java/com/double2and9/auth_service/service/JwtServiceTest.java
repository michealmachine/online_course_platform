package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.security.JwtTokenProvider;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private JwtService jwtService;

    private static final String TEST_TOKEN = "test.token.string";
    private Claims accessTokenClaims;
    private Claims refreshTokenClaims;

    @BeforeEach
    void setUp() {
        // 设置访问令牌的claims
        accessTokenClaims = Jwts.claims();
        accessTokenClaims.put("userId", "testUser");
        accessTokenClaims.put("clientId", "testClient");
        accessTokenClaims.put("scope", "read write");
        accessTokenClaims.put("type", "access_token");

        // 设置刷新令牌的claims
        refreshTokenClaims = Jwts.claims();
        refreshTokenClaims.put("userId", "testUser");
        refreshTokenClaims.put("clientId", "testClient");
        refreshTokenClaims.put("scope", "read write");
        refreshTokenClaims.put("type", "refresh_token");
    }

    @Test
    void generateAccessToken_Success() {
        when(jwtTokenProvider.generateToken(any(), eq(3600L))).thenReturn(TEST_TOKEN);

        String token = jwtService.generateAccessToken("testUser", "testClient", "read write");

        assertEquals(TEST_TOKEN, token);
    }

    @Test
    void generateRefreshToken_Success() {
        // 准备测试数据
        String userId = "test_user";
        String clientId = "test_client";
        String scope = "read write";

        // Mock JWT provider
        when(jwtTokenProvider.generateToken(argThat(claims -> 
            "test_user".equals(claims.get("userId")) &&
            "test_client".equals(claims.get("clientId")) &&
            "read write".equals(claims.get("scope")) &&
            "refresh_token".equals(claims.get("type"))
        ), eq(2592000L))).thenReturn(TEST_TOKEN);

        // 执行测试
        String refreshToken = jwtService.generateRefreshToken(userId, clientId, scope);

        // 验证结果
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
    void validateRefreshToken_Success() throws Exception {
        // Mock JwtTokenProvider 的行为
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(refreshTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);

        // 执行测试
        Claims result = jwtService.validateRefreshToken(TEST_TOKEN);

        // 验证结果
        assertNotNull(result);
        assertEquals("testUser", result.get("userId"));
        assertEquals("testClient", result.get("clientId"));
        assertEquals("refresh_token", result.get("type"));
    }

    @Test
    void validateRefreshToken_WrongTokenType() {
        // Mock JwtTokenProvider 的行为 - 返回访问令牌的claims
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);

        // 执行测试并验证异常
        AuthException exception = assertThrows(AuthException.class,
            () -> jwtService.validateRefreshToken(TEST_TOKEN));
        assertEquals(AuthErrorCode.TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    void getUserIdFromToken_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);

        String userId = jwtService.getUserIdFromToken(TEST_TOKEN);

        assertEquals("testUser", userId);
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
        // 准备测试数据
        String token = TEST_TOKEN;
        Claims claims = Jwts.claims();
        claims.setExpiration(new Date(System.currentTimeMillis() + 3600000)); // 1小时后过期

        // Mock JWT验证
        when(jwtTokenProvider.validateToken(token)).thenReturn(claims);

        // 执行测试
        jwtService.revokeToken(token);

        // 验证结果
        verify(tokenBlacklistService).addToBlacklist(eq(token), anyLong());
    }

    @Test
    void revokeToken_InvalidToken() {
        // 准备测试数据
        String token = "invalid.token";

        // Mock JWT验证失败
        when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException());

        // 执行测试并验证结果
        assertThrows(AuthException.class, () -> jwtService.revokeToken(token));
    }

    @Test
    void parseToken_RevokedToken() {
        // 准备测试数据
        String token = TEST_TOKEN;

        // Mock 令牌已被撤销
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(true);

        // 执行测试并验证结果
        AuthException exception = assertThrows(AuthException.class, () -> jwtService.parseToken(token));
        assertEquals(AuthErrorCode.TOKEN_REVOKED, exception.getErrorCode());
    }

    @Test
    void refreshTokens_Success() {
        // Mock JwtTokenProvider 的行为
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(refreshTokenClaims);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);
        
        // Mock 生成新令牌
        when(jwtTokenProvider.generateToken(any(), anyLong())).thenReturn(TEST_TOKEN);

        // 执行测试
        TokenResponse response = jwtService.refreshTokens(TEST_TOKEN);

        // 验证结果
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("read write", response.getScope());
        
        // 验证方法调用
        verify(jwtTokenProvider, times(2)).generateToken(any(), anyLong());
    }
} 