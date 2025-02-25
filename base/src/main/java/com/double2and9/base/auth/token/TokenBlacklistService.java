package com.double2and9.base.auth.token;

/**
 * 令牌黑名单服务接口
 * 用于管理已撤销但未过期的令牌
 */
public interface TokenBlacklistService {
    
    /**
     * 将令牌添加到黑名单
     *
     * @param token 令牌
     * @param expirationTime 过期时间(秒)
     */
    void addToBlacklist(String token, long expirationTime);
    
    /**
     * 检查令牌是否在黑名单中
     *
     * @param token 令牌
     * @return 是否在黑名单中
     */
    boolean isBlacklisted(String token);
} 