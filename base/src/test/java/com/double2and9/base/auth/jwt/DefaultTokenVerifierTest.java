package com.double2and9.base.auth.jwt;

import com.double2and9.base.auth.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultTokenVerifierTest {

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private DefaultTokenVerifier tokenVerifier;

    private static final String TEST_SECRET = "test-secret";
    private static final String TEST_TOKEN = "test.token.string";

    @BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn(TEST_SECRET);
    }

    @Test
    void shouldReturnTrueWhenTokenIsValid() {
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            // 准备测试数据
            Claims claims = new DefaultClaims();
            jwtUtils.when(() -> JwtUtils.validateToken(eq(TEST_TOKEN), eq(TEST_SECRET)))
                    .thenReturn(claims);
            
            // 执行测试
            boolean result = tokenVerifier.isTokenValid(TEST_TOKEN);
            
            // 验证
            assertTrue(result);
        }
    }

    @Test
    void shouldReturnFalseWhenTokenIsInvalid() {
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            // 准备测试数据
            jwtUtils.when(() -> JwtUtils.validateToken(eq(TEST_TOKEN), eq(TEST_SECRET)))
                    .thenThrow(new RuntimeException("Invalid token"));
            
            // 执行测试
            boolean result = tokenVerifier.isTokenValid(TEST_TOKEN);
            
            // 验证
            assertFalse(result);
        }
    }

    @Test
    void shouldReturnFalseForNullToken() {
        // 执行测试
        boolean result = tokenVerifier.isTokenValid(null);
        
        // 验证
        assertFalse(result);
    }

    @Test
    void shouldReturnFalseForEmptyToken() {
        // 执行测试
        boolean result = tokenVerifier.isTokenValid("");
        
        // 验证
        assertFalse(result);
    }

    @Test
    void shouldParseToken() {
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            // 准备测试数据
            Claims claims = new DefaultClaims();
            jwtUtils.when(() -> JwtUtils.validateToken(eq(TEST_TOKEN), eq(TEST_SECRET)))
                    .thenReturn(claims);
            
            // 执行测试
            Claims result = tokenVerifier.parseToken(TEST_TOKEN);
            
            // 验证
            assertNotNull(result);
            assertEquals(claims, result);
        }
    }

    @Test
    void shouldThrowExceptionForNullTokenWhenParsing() {
        // 执行测试和验证
        assertThrows(IllegalArgumentException.class, () -> tokenVerifier.parseToken(null));
    }

    @Test
    void shouldThrowExceptionForEmptyTokenWhenParsing() {
        // 执行测试和验证
        assertThrows(IllegalArgumentException.class, () -> tokenVerifier.parseToken(""));
    }

    @Test
    void shouldGetUsernameFromToken() {
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            // 准备测试数据
            Claims claims = new DefaultClaims();
            claims.setSubject("testuser");
            jwtUtils.when(() -> JwtUtils.validateToken(eq(TEST_TOKEN), eq(TEST_SECRET)))
                    .thenReturn(claims);
            
            // 执行测试
            String username = tokenVerifier.getUsernameFromToken(TEST_TOKEN);
            
            // 验证
            assertEquals("testuser", username);
        }
    }

    @Test
    void shouldThrowExceptionForNullTokenWhenGettingUsername() {
        // 执行测试和验证
        assertThrows(IllegalArgumentException.class, () -> tokenVerifier.getUsernameFromToken(null));
    }

    @Test
    void shouldGetClientIdFromToken() {
        try (MockedStatic<JwtUtils> jwtUtils = mockStatic(JwtUtils.class)) {
            // 准备测试数据
            Claims claims = new DefaultClaims();
            Map<String, Object> claimsMap = new HashMap<>();
            claimsMap.put("clientId", "test-client");
            claims.putAll(claimsMap);
            
            jwtUtils.when(() -> JwtUtils.validateToken(eq(TEST_TOKEN), eq(TEST_SECRET)))
                    .thenReturn(claims);
            
            // 执行测试
            String clientId = tokenVerifier.getClientIdFromToken(TEST_TOKEN, "clientId");
            
            // 验证
            assertEquals("test-client", clientId);
        }
    }

    @Test
    void shouldThrowExceptionForNullTokenWhenGettingClientId() {
        // 执行测试和验证
        assertThrows(IllegalArgumentException.class, 
                    () -> tokenVerifier.getClientIdFromToken(null, "clientId"));
    }

    @Test
    void shouldThrowExceptionForNullClientIdClaimName() {
        // 执行测试和验证
        assertThrows(IllegalArgumentException.class, 
                    () -> tokenVerifier.getClientIdFromToken(TEST_TOKEN, null));
    }
} 