package com.double2and9.base.auth.jwt;

import io.jsonwebtoken.Claims;

/**
 * 令牌验证接口
 */
public interface TokenVerifier {
    
    /**
     * 检查令牌是否有效
     *
     * @param token 令牌
     * @return 是否有效
     */
    boolean isTokenValid(String token);
    
    /**
     * 解析令牌
     *
     * @param token 令牌
     * @return 令牌声明
     * @throws RuntimeException 如果令牌无效
     */
    Claims parseToken(String token);
    
    /**
     * 从令牌获取用户名
     *
     * @param token 令牌
     * @return 用户名
     */
    String getUsernameFromToken(String token);
    
    /**
     * 从令牌获取客户端ID
     *
     * @param token 令牌
     * @param clientIdClaimName 客户端ID在声明中的名称
     * @return 客户端ID
     */
    String getClientIdFromToken(String token, String clientIdClaimName);
} 