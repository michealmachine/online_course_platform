package com.double2and9.base.auth.jwt;

/**
 * JWT配置接口
 * 用于提供JWT相关配置
 */
public interface JwtProperties {
    
    /**
     * 获取JWT签名密钥
     *
     * @return 签名密钥
     */
    String getSecret();
    
    /**
     * 获取访问令牌过期时间(秒)
     *
     * @return 过期时间
     */
    long getAccessTokenExpirationSeconds();
    
    /**
     * 获取刷新令牌过期时间(秒)
     *
     * @return 过期时间
     */
    long getRefreshTokenExpirationSeconds();
} 