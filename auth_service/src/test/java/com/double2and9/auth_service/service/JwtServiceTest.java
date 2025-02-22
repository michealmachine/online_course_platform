package com.double2and9.auth_service.service;

import com.double2and9.auth_service.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

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
    void validateRefreshToken_Success() {
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(refreshTokenClaims);

        Claims claims = jwtService.validateRefreshToken(TEST_TOKEN);

        assertEquals(refreshTokenClaims, claims);
    }

    @Test
    void validateRefreshToken_InvalidType() {
        Claims claims = Jwts.claims();
        claims.put("userId", "testUser");
        claims.put("clientId", "testClient");
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(claims);

        assertThrows(IllegalArgumentException.class, () -> 
            jwtService.validateRefreshToken(TEST_TOKEN));
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
} 