package com.double2and9.auth_service.service;

import com.double2and9.auth_service.dto.response.TokenIntrospectionResponse;
import com.double2and9.auth_service.dto.response.TokenResponse;
import com.double2and9.auth_service.exception.AuthException;
import com.double2and9.auth_service.security.AuthJwtTokenProvider;
import com.double2and9.base.enums.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
        accessTokenClaims.setExpiration(new Date(System.currentTimeMillis() + 3600000));

        // 设置刷新令牌的claims
        refreshTokenClaims = Jwts.claims();
        refreshTokenClaims.put("userId", "testUser");
        refreshTokenClaims.put("clientId", "testClient");
        refreshTokenClaims.put("scope", "read write");
        refreshTokenClaims.put("type", "refresh_token");
        refreshTokenClaims.setExpiration(new Date(System.currentTimeMillis() + 3600000));

        // 设置默认的Mock行为
        when(jwtTokenProvider.generateToken(anyMap(), anyLong())).thenReturn(TEST_TOKEN);
        when(tokenBlacklistService.isBlacklisted(TEST_TOKEN)).thenReturn(false);
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(accessTokenClaims);
    }

    @Test
    void generateAccessToken_Success() {
        String userId = "testUser";
        String clientId = "testClient";
        String scope = "read write";

        when(jwtTokenProvider.generateToken(argThat(claims -> 
            "testUser".equals(claims.get("userId")) &&
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
        String userId = "testUser";
        String clientId = "testClient";
        String scope = "read write";

        when(jwtTokenProvider.generateToken(argThat(claims -> 
            "testUser".equals(claims.get("userId")) &&
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
        assertEquals("testUser", result.get("userId"));
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
} 