package com.double2and9.auth_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    /**
     * 将令牌加入黑名单
     * @param token 令牌
     * @param expirationTime 过期时间（秒）
     */
    public void addToBlacklist(String token, long expirationTime) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", expirationTime, TimeUnit.SECONDS);
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
} 