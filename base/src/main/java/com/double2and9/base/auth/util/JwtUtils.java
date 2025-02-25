package com.double2and9.base.auth.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
public class JwtUtils {
    
    /**
     * 生成JWT令牌
     * 
     * @param secret 签名密钥
     * @param claims 令牌载荷
     * @param expirationSeconds 过期时间(秒)
     * @return JWT令牌字符串
     */
    public static String generateToken(String secret, Map<String, Object> claims, long expirationSeconds) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(secret), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 验证令牌并返回声明
     * 
     * @param token JWT令牌
     * @param secret 签名密钥
     * @return 令牌声明
     * @throws JwtException 如果令牌无效
     */
    public static Claims validateToken(String token, String secret) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 解析令牌但不抛出异常
     * 
     * @param token JWT令牌
     * @param secret 签名密钥
     * @return 令牌声明，如果令牌无效则返回null
     */
    public static Claims parseToken(String token, String secret) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        try {
            return validateToken(token, secret);
        } catch (Exception e) {
            log.debug("Token parsing failed: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从令牌中提取用户名
     */
    public static String getUsernameFromToken(String token, String secret) {
        Claims claims = validateToken(token, secret);
        return claims.getSubject();
    }
    
    /**
     * 从令牌中提取客户端ID
     */
    public static String getClientIdFromToken(String token, String secret, String clientIdClaimName) {
        Claims claims = validateToken(token, secret);
        return (String) claims.get(clientIdClaimName);
    }
    
    /**
     * 检查令牌是否过期
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            Claims claims = validateToken(token, secret);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * 获取签名密钥
     */
    private static Key getSigningKey(String secret) {
        byte[] keyBytes = Decoders.BASE64URL.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
} 