package com.double2and9.auth_service.security;

import com.double2and9.base.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "testSecretKeyWithLength512BitsForTestingPurposesOnly";
    private static final long TEST_EXPIRATION = 3600000L;

    private JwtProperties jwtProperties;
    private AuthJwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpiration(TEST_EXPIRATION);
        
        jwtTokenProvider = new AuthJwtTokenProvider(jwtProperties);
    }

    @Test
    void generateToken_Success() {
        // 准备测试数据
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "123");
        claims.put("username", "testuser");
        
        // 生成令牌
        String token = jwtTokenProvider.generateToken(claims, 3600L);
        
        // 验证结果
        assertNotNull(token);
        assertTrue(token.length() > 0);
        
        // 验证令牌内容
        Claims validatedClaims = jwtTokenProvider.validateToken(token);
        assertEquals("123", validatedClaims.get("userId"));
        assertEquals("testuser", validatedClaims.get("username"));
    }

    @Test
    void validateToken_Success() {
        // 准备测试数据
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", "123");
        claims.put("username", "testuser");
        
        // 生成令牌
        String token = jwtTokenProvider.generateToken(claims, 3600L);
        
        // 验证令牌
        Claims validatedClaims = jwtTokenProvider.validateToken(token);
        
        // 验证结果
        assertNotNull(validatedClaims);
        assertEquals("123", validatedClaims.get("userId"));
        assertEquals("testuser", validatedClaims.get("username"));
    }

    @Test
    void validateToken_InvalidToken() {
        // 准备无效的令牌
        String invalidToken = "invalid.token.string";
        
        // 验证是否抛出异常
        assertThrows(RuntimeException.class, () -> 
            jwtTokenProvider.validateToken(invalidToken));
    }
} 