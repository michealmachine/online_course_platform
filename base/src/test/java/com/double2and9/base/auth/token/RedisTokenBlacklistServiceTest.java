package com.double2and9.base.auth.token;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisTokenBlacklistService blacklistService;

    @Test
    void shouldAddTokenToBlacklist() {
        // 准备测试数据
        String token = "test.token.string";
        long expirationTime = 3600;
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // 执行测试
        blacklistService.addToBlacklist(token, expirationTime);
        
        // 验证
        verify(valueOperations).set(eq("token:blacklist:test.token.string"), eq("revoked"), eq(expirationTime), eq(TimeUnit.SECONDS));
    }

    @Test
    void shouldCheckIfTokenIsBlacklisted() {
        // 准备测试数据
        String token = "test.token.string";
        
        when(redisTemplate.hasKey("token:blacklist:test.token.string")).thenReturn(true);
        
        // 执行测试
        boolean result = blacklistService.isBlacklisted(token);
        
        // 验证
        assertTrue(result);
    }

    @Test
    void shouldReturnFalseForNonBlacklistedToken() {
        // 准备测试数据
        String token = "test.token.string";
        
        when(redisTemplate.hasKey("token:blacklist:test.token.string")).thenReturn(false);
        
        // 执行测试
        boolean result = blacklistService.isBlacklisted(token);
        
        // 验证
        assertFalse(result);
    }
    
    @Test
    void shouldReturnFalseForNullToken() {
        // 执行测试
        boolean result = blacklistService.isBlacklisted(null);
        
        // 验证
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(any());
    }
    
    @Test
    void shouldReturnFalseForEmptyToken() {
        // 执行测试
        boolean result = blacklistService.isBlacklisted("");
        
        // 验证
        assertFalse(result);
        verify(redisTemplate, never()).hasKey(any());
    }
} 