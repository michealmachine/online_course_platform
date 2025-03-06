package com.double2and9.auth_service.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌黑名单服务的测试模拟实现
 * 使用内存Map存储而不是Redis
 */
@Service
public class MockTokenBlacklistService extends TokenBlacklistService {
    
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    
    /**
     * 构造函数
     * 传入mock的RedisTemplate以满足父类构造器要求
     */
    public MockTokenBlacklistService() {
        super(new NoOpRedisTemplate<>());
    }
    
    /**
     * 将令牌加入黑名单
     * @param token 令牌
     * @param expirationTime 过期时间（秒）
     */
    @Override
    public void addToBlacklist(String token, long expirationTime) {
        blacklistedTokens.put(token, System.currentTimeMillis() + (expirationTime * 1000));
        System.out.println("测试环境：令牌已加入黑名单: " + token);
    }
    
    /**
     * 检查令牌是否在黑名单中
     */
    @Override
    public boolean isBlacklisted(String token) {
        if (!blacklistedTokens.containsKey(token)) {
            return false;
        }
        
        // 检查是否已过期
        long expiryTime = blacklistedTokens.get(token);
        if (System.currentTimeMillis() > expiryTime) {
            blacklistedTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * 不执行任何操作的RedisTemplate实现
     * 所有方法都返回null或默认值，确保不会实际调用Redis操作
     */
    private static class NoOpRedisTemplate<K, V> extends RedisTemplate<K, V> {
        // RedisTemplate的所有方法都已有默认实现，返回null或抛出异常
        // 但在我们的测试中不会实际调用这些方法
    }
} 