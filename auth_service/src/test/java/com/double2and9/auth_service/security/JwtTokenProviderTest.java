package com.double2and9.auth_service.security;

import com.double2and9.auth_service.exception.AuthException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "ZDY1NmE3ZjI0ODdiNGZkZWJmZjk5MjExYjU4MzRlMzE2NzIwZWM0MjJmNmFlOWY5NzM2ZmRkMGM5NTYzYzE3YQ=="; // 使用 Keys.secretKeyFor(SignatureAlgorithm.HS512) 生成的密钥

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", TEST_SECRET);
    }

    @Test
    void parseToken_ValidToken() {
        // 准备测试数据
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "testUser");
        claims.put("clientId", "testClient");
        String token = jwtTokenProvider.generateToken(claims, 3600);

        // 执行测试
        Claims parsedClaims = jwtTokenProvider.parseToken(token);

        // 验证结果
        assertNotNull(parsedClaims);
        assertEquals("testUser", parsedClaims.get("userId"));
        assertEquals("testClient", parsedClaims.get("clientId"));
    }

    @Test
    void parseToken_InvalidToken() {
        // 准备无效的令牌
        String invalidToken = "invalid.jwt.token";

        // 执行测试并验证异常
        assertThrows(AuthException.class, () -> 
            jwtTokenProvider.parseToken(invalidToken));
    }

    @Test
    void parseToken_ExpiredToken() {
        // 生成一个已过期的令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "testUser");
        String expiredToken = jwtTokenProvider.generateToken(claims, -3600); // 负数表示过期

        // 执行测试并验证异常
        assertThrows(AuthException.class, () -> 
            jwtTokenProvider.parseToken(expiredToken));
    }

    @Test
    void parseToken_ModifiedToken() {
        // 生成有效令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "testUser");
        String token = jwtTokenProvider.generateToken(claims, 3600);

        // 修改令牌
        String modifiedToken = token + "modified";

        // 执行测试并验证异常
        assertThrows(AuthException.class, () -> 
            jwtTokenProvider.parseToken(modifiedToken));
    }

    @Test
    void parseToken_EmptyToken() {
        // 执行测试并验证异常
        assertThrows(AuthException.class, () -> 
            jwtTokenProvider.parseToken(""));
    }

    @Test
    void parseToken_NullToken() {
        // 执行测试并验证异常
        assertThrows(AuthException.class, () -> 
            jwtTokenProvider.parseToken(null));
    }
} 