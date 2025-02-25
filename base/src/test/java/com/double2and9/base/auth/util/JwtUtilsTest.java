package com.double2and9.base.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtils工具类的单元测试
 */
class JwtUtilsTest {

    private static final String TEST_SECRET = "ZDY1NmE3ZjI0ODdiNGZkZWJmZjk5MjExYjU4MzRlMzE2NzIwZWM0MjJmNmFlOWY5NzM2ZmRkMGM5NTYzYzE3YQ==";

    @Test
    void shouldGenerateAndValidateToken() {
        // 准备测试数据
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "testUser");
        claims.put("clientId", "testClient");
        claims.put("sub", "testUsername");
        
        // 生成令牌
        String token = JwtUtils.generateToken(TEST_SECRET, claims, 3600);
        
        // 验证令牌
        Claims parsedClaims = JwtUtils.validateToken(token, TEST_SECRET);
        
        // 断言
        assertNotNull(parsedClaims);
        assertEquals("testUser", parsedClaims.get("userId"));
        assertEquals("testClient", parsedClaims.get("clientId"));
        assertEquals("testUsername", parsedClaims.getSubject());
        assertNotNull(parsedClaims.getIssuedAt());
        assertNotNull(parsedClaims.getExpiration());
    }

    @Test
    void shouldReturnNullWhenParsingInvalidToken() {
        // 准备无效的令牌
        String invalidToken = "invalid.jwt.token";
        
        // 解析
        Claims claims = JwtUtils.parseToken(invalidToken, TEST_SECRET);
        
        // 断言
        assertNull(claims);
    }

    @Test
    void shouldReturnNullWhenParsingNullToken() {
        // 解析null令牌
        Claims claims = JwtUtils.parseToken(null, TEST_SECRET);
        
        // 断言
        assertNull(claims);
    }

    @Test
    void shouldThrowExceptionWhenTokenExpired() {
        // 准备过期的令牌 (-1秒意味着已经过期)
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "testUser");
        String expiredToken = JwtUtils.generateToken(TEST_SECRET, claims, -1);
        
        // 验证
        assertThrows(ExpiredJwtException.class, () -> JwtUtils.validateToken(expiredToken, TEST_SECRET));
    }
    
    @Test
    void shouldGetUsernameFromToken() {
        // 准备数据
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "testUsername");
        String token = JwtUtils.generateToken(TEST_SECRET, claims, 3600);
        
        // 获取用户名
        String username = JwtUtils.getUsernameFromToken(token, TEST_SECRET);
        
        // 断言
        assertEquals("testUsername", username);
    }
    
    @Test
    void shouldGetClientIdFromToken() {
        // 准备数据
        Map<String, Object> claims = new HashMap<>();
        claims.put("clientId", "testClient");
        String token = JwtUtils.generateToken(TEST_SECRET, claims, 3600);
        
        // 获取客户端ID
        String clientId = JwtUtils.getClientIdFromToken(token, TEST_SECRET, "clientId");
        
        // 断言
        assertEquals("testClient", clientId);
    }
} 