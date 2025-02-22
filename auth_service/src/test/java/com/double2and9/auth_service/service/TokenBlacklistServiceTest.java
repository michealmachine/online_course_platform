package com.double2and9.auth_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void addToBlacklist_Success() {
        String token = "test.token.string";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        tokenBlacklistService.addToBlacklist(token, 3600);

        verify(valueOperations).set(eq("token:blacklist:" + token), eq("revoked"), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    void isBlacklisted_True() {
        String token = "test.token.string";
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertTrue(result);
        verify(redisTemplate).hasKey("token:blacklist:" + token);
    }

    @Test
    void isBlacklisted_False() {
        String token = "test.token.string";
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        boolean result = tokenBlacklistService.isBlacklisted(token);

        assertFalse(result);
        verify(redisTemplate).hasKey("token:blacklist:" + token);
    }
} 